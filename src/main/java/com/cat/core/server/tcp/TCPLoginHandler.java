package com.cat.core.server.tcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.ErrNo;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.tcp.session.StateManager;
import com.cat.core.server.tcp.session.TCPInfo;
import com.cat.core.server.tcp.session.TCPSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 登录处理
 */
final class TCPLoginHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;

		Channel channel = ctx.channel();

		JSONObject json = JSON.parseObject(command);
		Action action = Action.from(json.getString(Key.ACTION.getName()));

		//登录请求
		if (action == Action.LOGIN_REQUEST) {
			ready(channel, json);
			return;
		}

		//登录验证
		Result result = Result.from(json.getString(Key.RESULT.getName()));
		String keyCode = json.getString(Key.KEYCODE.getName());
		if (result == Result.OK && keyCode != null) {
			verify(channel, keyCode);
			return;
		}

		//拦截未登录的连接
		if (TCPSessionManager.passed(channel)) {
			ctx.fireChannelRead(command);
		} else {
			TCPSessionManager.close(channel);
		}

	}

	/**
	 * 处理登录请求(准备阶段)
	 */
	private void ready(Channel channel, JSONObject json) {
		JSONObject response = new JSONObject();

		TCPInfo info = TCPInfo.from(json);

		String question = StateManager.request(channel, info);
		if (question == null) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.PARAMETER.getCode());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		response.put(Key.ACTION.getName(), Action.LOGIN_VERIFY.getName());
		response.put(Key.KEY.getName(), question);

		channel.writeAndFlush(response);
	}

	/**
	 * 处理登录请求(验证阶段)
	 *
	 * @param answer 客户端答复
	 */
	private void verify(Channel channel, String answer) {
		JSONObject response = new JSONObject();

		if (!StateManager.verify(channel, answer)) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.UNKNOWN.getCode());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		int allocated = StateManager.pass(channel);

		if (allocated == -1) {
			response.put(Key.RESULT.getName(), Result.NO.getName());
			response.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
			channel.writeAndFlush(response);
			TCPSessionManager.close(channel);
			return;
		}

		response.put(Key.RESULT.getName(), Result.OK.getName());

		if (allocated > 0) {
			response.put(Key.ALLOT.getName(), allocated);//网关登录
		}
		channel.writeAndFlush(response);
	}
}
