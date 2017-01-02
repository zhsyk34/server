package com.cat.core.server.udp.session;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.tcp.message.UDPPusher;
import com.cat.core.server.udp.UDPServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UDP心跳管理
 */
public final class UDPManager {
	/**
	 * 网关UDP心跳记录,key=sn
	 */
	private static final Map<String, UDPInfo> GATEWAY_INFO_MAP = new ConcurrentHashMap<>();

	public static UDPInfo find(String sn) {
		return GATEWAY_INFO_MAP.get(sn);
	}

	/**
	 * 缓存接收的网关心跳信息
	 */
	public static void receive(UDPInfo info) {
		GATEWAY_INFO_MAP.put(info.getSn(), info);
	}

	/**
	 * 响应网关心跳
	 */
	public static void response(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());
		send(target, json);
	}

	public static void awake(String host, int port) {
		awake(new InetSocketAddress(host, port));
	}

	private static void awake(InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.ACTION.getName(), Action.LOGIN_INFORM.getName());
		send(target, json);
	}

	/**
	 * 清理过期的数据
	 */
	public static void monitor() {
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
	 * @param target 目标地址
	 * @param json   JSON数据
	 */
	private static void send(InetSocketAddress target, JSONObject json) {
		if (UDPServer.getChannel() == null) {
			return;
		}
		ByteBuf buf = Unpooled.copiedBuffer(json.toString().getBytes(CharsetUtil.UTF_8));
		UDPServer.getChannel().writeAndFlush(new DatagramPacket(buf, target));
	}

	/**
	 * 推送udp信息至web服务器
	 */
	public static void push() {
		List<UDPInfo> list = new ArrayList<>(GATEWAY_INFO_MAP.values());

		final int batch = 10;

		for (int i = 0; i < list.size(); i += batch) {
			JSONObject json = new JSONObject();
			json.put(Key.ACTION.getName(), Action.UDP_SESSION_PUSH.getName());
			json.put(Key.DATA.getName(), list.subList(i, Math.min(i + batch, list.size())));
			UDPPusher.push(json.toString());
		}
	}
}
