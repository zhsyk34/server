package com.cat.core.server.udp;

import com.cat.core.dict.Packet;
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
		String command = msg.content().toString(CharsetUtil.UTF_8);
		out.add(msg.replace(Unpooled.wrappedBuffer(CodecKit.encode(command))));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		ByteBuf buf = msg.content();
		ByteBuf command = buf.slice(Packet.HEADERS.size() + Packet.LENGTH_BYTES, buf.readableBytes() - Packet.REDUNDANT_BYTES);
		out.add(msg.replace(CodecKit.decode(command)));
	}
}
