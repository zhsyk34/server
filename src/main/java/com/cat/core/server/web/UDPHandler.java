package com.cat.core.server.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.Controller;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * receive the push response
 */
final class UDPHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private final Controller controller = Controller.instance();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		String receive = msg.content().toString(CharsetUtil.UTF_8);

		JSONObject json = JSON.parseObject(receive);
		String sn = json.getString(Key.SN.getName());
		Result result = Result.from(json.getString(Key.RESULT.getName()));

		//get version
		if (result == Result.OK && sn != null) {
			json.remove(Key.SN.getName());
			controller.version(sn, json);
		}
	}
}
