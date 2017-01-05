package com.cat.core.server.tcp;

import com.cat.core.config.Config;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class TCPServer {

	private static final Lock lock = new ReentrantLock();

	@Getter
	private static volatile boolean started = false;

	public static void start() {
		lock.lock();
		if (started) {
			return;
		}

		ServerBootstrap bootstrap = new ServerBootstrap();

		EventLoopGroup mainGroup = new NioEventLoopGroup();
		EventLoopGroup handleGroup = new NioEventLoopGroup();

		bootstrap.group(mainGroup, handleGroup).channel(NioServerSocketChannel.class);

		//setting options
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_BACKLOG, Config.TCP_SERVER_BACKLOG);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Config.TCP_CONNECT_TIMEOUT * 1000);

		//pool
		bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

		//logging
		bootstrap.childHandler(new LoggingHandler(LogLevel.WARN));

		//handler
		bootstrap.childHandler(new TCPInitializer());

		try {
			ChannelFuture future = bootstrap.bind(Config.TCP_SERVER_HOST, Config.TCP_SERVER_PORT).sync();

			started = true;
			lock.unlock();

			Log.logger(Factory.TCP_EVENT, TCPServer.class.getSimpleName() + " start success at port[" + Config.TCP_SERVER_PORT + "]");

			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			lock.unlock();
			e.printStackTrace();
		} finally {
			lock.unlock();
			mainGroup.shutdownGracefully();
			handleGroup.shutdownGracefully();
		}
	}

	private final static class TCPInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new TCPInitHandler());
			pipeline.addLast(new TCPDecoder());
			pipeline.addLast(new TCPEncoder());
			pipeline.addLast(new TCPLoginHandler());
			pipeline.addLast(new TCPServerHandler());
		}
	}
}
