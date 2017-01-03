package com.cat.core.server.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * TCP初始化时注册监听事件
 */
final class TCPInitHandler extends ChannelInboundHandlerAdapter {

	private final Manager handler = Manager.instance();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		handler.create(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		handler.close(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		this.channelInactive(ctx);
	}

	@Override
	public boolean isSharable() {
		return true;
	}
}