package com.cat.core.server.tcp;

import com.cat.core.server.tcp.session.DefaultEventHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * TCP初始化时注册监听事件
 */
final class TCPInitHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		DefaultEventHandler.instance().create(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		DefaultEventHandler.instance().close(ctx.channel());
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
