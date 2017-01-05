package com.cat.core.server.tcp;

import com.cat.core.server.Controller;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

final class TCPInitHandler extends ChannelInboundHandlerAdapter {

	private final Controller controller = Controller.instance();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		controller.create(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		controller.close(ctx.channel());
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
