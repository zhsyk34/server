package com.cat.core.server.udp.session;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import com.cat.core.server.data.Action;
import com.cat.core.server.data.Key;
import com.cat.core.server.data.Result;
import com.cat.core.server.udp.UDPServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * udp server manager
 */
public final class UDPManager {

	/**
	 * gateway udp session info,the key is gateway-sn
	 */
	private static final Map<String, UDPInfo> GATEWAY_INFO_MAP = new ConcurrentHashMap<>();

	public static UDPInfo find(String sn) {
		return GATEWAY_INFO_MAP.get(sn);
	}

	public static void receive(UDPInfo info) {
		GATEWAY_INFO_MAP.put(info.getSn(), info);
	}

	/**
	 * response udp heart beat
	 */
	public static void response(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());
		send(target, json);
	}

	/**
	 * send message to awake gateway login
	 */
	public static void awake(String host, int port) {
		awake(new InetSocketAddress(host, port));
	}

	public static void awake(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.ACTION.getName(), Action.LOGIN_INFORM.getName());
		send(target, json);
	}

	private static void send(InetSocketAddress target, JSONObject json) {
		Channel channel = UDPServer.getChannel();

		if (channel != null) {
			ByteBuf buf = Unpooled.copiedBuffer(json.toString().getBytes(CharsetUtil.UTF_8));
			channel.writeAndFlush(new DatagramPacket(buf, target));
		}
	}

	/**
	 * clean stale data
	 */
	public static void clean() {
		Iterator<Map.Entry<String, UDPInfo>> iterator = GATEWAY_INFO_MAP.entrySet().iterator();
		while (iterator.hasNext()) {
			UDPInfo info = iterator.next().getValue();
			long createTime = info.getHappen();
			if (!ValidateKit.time(createTime, Config.UDP_HEART_DUE)) {
				iterator.remove();
			}
		}
	}

	/**
	 * push session data to web server by udp
	 */
	public static void push() {
		List<UDPInfo> list = new ArrayList<>(GATEWAY_INFO_MAP.values());

		//TODO
		final int batch = 10;

		for (int i = 0; i < list.size(); i += batch) {
			JSONObject json = new JSONObject();
			json.put(Key.ACTION.getName(), Action.UDP_SESSION_PUSH.getName());
			json.put(Key.DATA.getName(), list.subList(i, Math.min(i + batch, list.size())));
//			UDPClient.push(json.toString());
		}
	}
}
