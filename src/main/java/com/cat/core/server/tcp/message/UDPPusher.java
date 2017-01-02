package com.cat.core.server.tcp.message;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * UDP客户端(发送器),推送信息到web服务器
 */
public final class UDPPusher {

	private static final Lock lock = new ReentrantLock();

	@Getter
	private static Channel channel;

	public static synchronized void start() {
		lock.lock();
		if (channel != null) {
			return;
		}

		Bootstrap bootstrap = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			bootstrap.group(group).channel(NioDatagramChannel.class);
			bootstrap.option(ChannelOption.SO_BROADCAST, false);
			bootstrap.handler(new ChannelInitializer<DatagramChannel>() {
				@Override
				protected void initChannel(DatagramChannel ch) throws Exception {
				}
			});

			channel = bootstrap.bind(Config.UDP_PUSHER_PORT).syncUninterruptibly().channel();

			lock.unlock();

			Log.logger(Factory.UDP_EVENT, UDPPusher.class.getSimpleName() + " 在端口[" + Config.UDP_PUSHER_PORT + "]启动完毕");
			channel.closeFuture().await();
		} catch (Exception e) {
			lock.unlock();
			e.printStackTrace();
		} finally {
			lock.unlock();
			group.shutdownGracefully();
		}
	}

	public static boolean push(@NonNull JSONObject msg) {
		return push(msg.toString());
	}

	public static boolean push(@NonNull String msg) {
		if (channel == null) {
			return false;
		}
		InetSocketAddress target = new InetSocketAddress(Config.UDP_WEB_IP, Config.UDP_WEB_PORT);
		ByteBuf buf = Unpooled.copiedBuffer(msg.getBytes(CharsetUtil.UTF_8));
		channel.writeAndFlush(new DatagramPacket(buf, target));
		return true;
	}
}
