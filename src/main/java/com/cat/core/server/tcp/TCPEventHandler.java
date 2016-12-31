package com.cat.core.server.tcp;

import com.cat.core.server.tcp.session.TCPSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 监听TCP连接事件
 */
final class TCPEventHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		TCPSessionManager.init(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		TCPSessionManager.close(ctx.channel());
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
