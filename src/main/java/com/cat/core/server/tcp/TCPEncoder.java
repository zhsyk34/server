package com.cat.core.server.tcp;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.kit.CodecKit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * TCP服务器发送数据前进行编码(加密等)
 * 支持直接发送 {@link String} 或 {@link JSONObject}
 */
final class TCPEncoder extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		String result = null;
		if (msg instanceof String) {
			result = (String) msg;
		} else if (msg instanceof JSONObject) {
			result = msg.toString();
		}

		if (result != null) {
			byte[] data = CodecKit.encode(result);
			out.writeBytes(Unpooled.wrappedBuffer(data));
		}
	}
}
