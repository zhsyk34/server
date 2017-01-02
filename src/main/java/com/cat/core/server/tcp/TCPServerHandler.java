package com.cat.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.kit.JsonKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.tcp.message.AppRequest;
import com.cat.core.server.tcp.message.MessageManager;
import com.cat.core.server.tcp.session.DefaultEventHandler;
import com.cat.core.server.tcp.session.LoginInfo;
import com.cat.core.server.tcp.session.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 处理除登录以外的请求,登录验证已在此前的 {@link TCPLoginHandler} 中处理
 * <p>
 * 1.网关心跳:直接回复
 * 2.网关推送信息:直接通过UDP转发到WEB服务器
 * 3.app控制指令与网关响应信息交由 {@link com.cat.core.server.tcp.message.MessageManager} 统一管理
 */
final class TCPServerHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;

		JSONObject json = JsonKit.map(command);
		Action action = Action.from(json.getString(Key.ACTION.getName()));
		Result result = Result.from(json.getString(Key.RESULT.getName()));

		if (action == null && result == null) {
//			Log.logger(Factory.TCP_EVENT, "无效的指令:\n" + command);
			return;
		}

		Channel channel = ctx.channel();
		LoginInfo info = DefaultEventHandler.instance().info(channel);
		String sn = info.getSn();

		switch (info.getDevice()) {
			case APP:
				Log.logger(Factory.TCP_RECEIVE, "客户端请求[" + command + "],将其添加到消息处理队列...");
				MessageManager.register(sn, AppRequest.of(SessionManager.id(channel), command));
				break;
			case GATEWAY:
				//1.心跳
				if (action == Action.HEART_BEAT) {
					Log.logger(Factory.TCP_RECEIVE, "网关[" + info.getSn() + "] 发送心跳");
					JSONObject heartResp = new JSONObject();
					heartResp.put(Key.RESULT.getName(), Result.OK.getName());
					channel.writeAndFlush(heartResp);
					return;
				}

				//2.推送
				if (action != null && action.getType() == 4) {
					Log.logger(Factory.TCP_RECEIVE, "网关[" + sn + "]推送数据...");
					MessageManager.push(sn, command);
					return;
				}

				//3.响应请求
				if (result != null) {
					Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]回复app请求,转发...");
					MessageManager.response(sn, command);
				}
				break;
			default:
				break;
		}
	}
}
