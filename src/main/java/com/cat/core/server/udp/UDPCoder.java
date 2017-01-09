package com.cat.core.server.udp;

import com.cat.core.dict.Protocol;
import com.cat.core.kit.CodecKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

import java.util.List;

final class UDPCoder extends MessageToMessageCodec<DatagramPacket, DatagramPacket> {
	@Override
	protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		String content = msg.content().toString(CharsetUtil.UTF_8);
		out.add(msg.replace(Unpooled.wrappedBuffer(CodecKit.encode(content))));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		ByteBuf content = msg.content();
		ByteBuf buf = content.slice(Protocol.HEADERS.size() + Protocol.LENGTH_BYTES, content.readableBytes() - Protocol.REDUNDANT_BYTES);
		out.add(msg.replace(CodecKit.decode(buf)));
	}
}
