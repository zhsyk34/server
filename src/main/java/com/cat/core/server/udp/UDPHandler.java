package com.cat.core.server.udp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.kit.ValidateKit;
import com.cat.core.server.Controller;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.udp.session.UDPInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

/**
 * validate the receive message is correct heart beat
 * if yes then save and response it
 */
final class UDPHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private final Controller controller = Controller.instance();

	private UDPInfo validate(DatagramPacket msg) {
		String command = msg.content().toString(CharsetUtil.UTF_8);

		JSONObject json = JSON.parseObject(command);

		//1.
		Result result = Result.from(json.getString(Key.RESULT.getName()));
		if (result == Result.OK) {
			return null;
		}

		//2.
		Action action = Action.from(json.getString(Key.ACTION.getName()));
		String sn = json.getString(Key.SN.getName());
		String version = json.getString(Key.VERSION.getName());
		if (action != Action.HEART_BEAT || ValidateKit.isEmpty(sn, version)) {
			return null;
		}

		return UDPInfo.from(sn, msg.sender(), version);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		UDPInfo info = validate(msg);
		if (info != null) {
			controller.receive(info);
			controller.response(msg.sender());
		}
	}
}
