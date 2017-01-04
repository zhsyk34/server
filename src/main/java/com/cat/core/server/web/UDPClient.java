package com.cat.core.server.web;

import com.cat.core.config.Config;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * udp client:push data to web service
 */
final class UDPClient {

	private static final Lock lock = new ReentrantLock();

	@Getter
	private static Channel channel;

	static void start() {
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

			Log.logger(Factory.UDP_EVENT, UDPClient.class.getSimpleName() + " 在端口[" + Config.UDP_PUSHER_PORT + "]启动完毕");
			channel.closeFuture().await();
		} catch (Exception e) {
			lock.unlock();
			e.printStackTrace();
		} finally {
			lock.unlock();
			group.shutdownGracefully();
		}
	}

}
