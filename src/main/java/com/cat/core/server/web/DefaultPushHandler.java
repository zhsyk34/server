package com.cat.core.server.web;

import com.cat.core.config.Config;
import com.cat.core.kit.ThreadKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NoArgsConstructor(staticName = "instance")
public final class DefaultPushHandler implements PushHandler {
	private static final InetSocketAddress WEB_UDP_SERVICE = new InetSocketAddress(Config.UDP_WEB_IP, Config.UDP_WEB_PORT);

	static {
		ExecutorService service = Executors.newSingleThreadExecutor();
		service.submit(UDPClient::start);
		service.shutdown();

		while (UDPClient.getChannel() == null) {
			Log.logger(Factory.UDP_EVENT, UDPClient.class.getSimpleName() + " is starting...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME);
		}
	}

	@Override
	public boolean push(@NonNull String msg) {
		Log.logger(Factory.UDP_EVENT, "push msg:" + msg);
		Channel channel = UDPClient.getChannel();
		if (channel == null) {
			return false;
		}

		ByteBuf buf = Unpooled.copiedBuffer(msg.getBytes(CharsetUtil.UTF_8));
		channel.writeAndFlush(new DatagramPacket(buf, WEB_UDP_SERVICE));
		return true;
	}

}
