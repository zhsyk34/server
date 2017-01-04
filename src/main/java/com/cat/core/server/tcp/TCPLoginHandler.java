package com.cat.core.server.tcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.data.Action;
import com.cat.core.server.data.Key;
import com.cat.core.server.data.Result;
import com.cat.core.server.tcp.state.LoginInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

final class TCPLoginHandler extends ChannelInboundHandlerAdapter {

	private final Controller handler = Controller.instance();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof String)) {
			return;
		}
		String command = (String) msg;

		Channel channel = ctx.channel();

		JSONObject json = JSON.parseObject(command);

		//1.session request
		Action action = Action.from(json.getString(Key.ACTION.getName()));

		if (action == Action.LOGIN_REQUEST) {
			handler.request(channel, LoginInfo.from(json));
			return;
		}

		//2.verify and allocate port2(gateway client)
		Result result = Result.from(json.getString(Key.RESULT.getName()));
		String keyCode = json.getString(Key.KEYCODE.getName());
		if (result == Result.OK && keyCode != null) {
			handler.verify(channel, keyCode);
			return;
		}

		//3.filter un-session client
		if (handler.finished(channel)) {
			ctx.fireChannelRead(command);
		} else {
			handler.close(channel);
		}
	}

}
