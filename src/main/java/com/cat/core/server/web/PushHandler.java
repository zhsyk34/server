package com.cat.core.server.web;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.InetSocketAddress;

@NoArgsConstructor(staticName = "instance")
public final class PushHandler {

	private static final InetSocketAddress WEB_UDP_SERVICE = new InetSocketAddress(Config.UDP_WEB_IP, Config.UDP_WEB_PORT);

	public boolean push(@NonNull String msg) {
		Channel channel = UDPClient.getChannel();
		if (channel == null) {
			return false;
		}

		ByteBuf buf = Unpooled.copiedBuffer(msg.getBytes(CharsetUtil.UTF_8));
		channel.writeAndFlush(new DatagramPacket(buf, WEB_UDP_SERVICE));
		return true;
	}

	public boolean push(@NonNull JSONObject msg) {
		return this.push(msg.toString());
	}

}
