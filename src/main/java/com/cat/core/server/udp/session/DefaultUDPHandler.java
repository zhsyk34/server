package com.cat.core.server.udp.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.task.FixedTimerTask;
import com.cat.core.server.task.LoopTask;
import com.cat.core.server.udp.UDPServer;
import com.cat.core.server.web.PushHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultUDPHandler implements UDPHandler {

	private static final Map<String, UDPInfo> GATEWAY_INFO_MAP = new ConcurrentHashMap<>();

	@NonNull
	private final PushHandler pushHandler;

	private static void send(InetSocketAddress target, JSONObject json) {
		Channel channel = UDPServer.getChannel();

		if (channel != null) {
			ByteBuf buf = Unpooled.copiedBuffer(json.toString().getBytes(CharsetUtil.UTF_8));
			channel.writeAndFlush(new DatagramPacket(buf, target));
		}
	}

	//TODO:size:107
	//MAX-SIZE:局域网1400+ inet:548
	//http://bbs.chinaunix.net/thread-1762376-1-1.html
	public static void main(String[] args) {
		UDPInfo info = UDPInfo.of("003-004-115-116", "111.111.111.111", 56789, "version1.12", System.currentTimeMillis());
		String msg = JSON.toJSONString(info);
		System.out.println(msg + " " + msg.length());
	}

	@Override
	public UDPInfo find(@NonNull String sn) {
		return GATEWAY_INFO_MAP.get(sn);
	}

	@Override
	public void receive(@NonNull UDPInfo info) {
		GATEWAY_INFO_MAP.put(info.getSn(), info);
	}

	@Override
	public void response(@NonNull InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());
		send(target, json);
	}

	@Override
	public void awake(@NonNull InetSocketAddress target) {
		JSONObject json = new JSONObject();
		json.put(Key.ACTION.getName(), Action.LOGIN_INFORM.getName());
		send(target, json);
	}

	@Override
	public FixedTimerTask clean() {
		return FixedTimerTask.of(() -> GATEWAY_INFO_MAP.entrySet().removeIf(entry -> !ValidateKit.time(entry.getValue().getHappen(), Config.UDP_HEART_DUE)), LocalTime.MIN, 1, TimeUnit.DAYS);
	}

	@Override
	public LoopTask push() {
		final int batch = Config.UDP_SESSION_PUSH_BATCH;
		return () -> {
			List<UDPInfo> list = new ArrayList<>(GATEWAY_INFO_MAP.values());

			for (int i = 0; i < list.size(); i += batch) {
				JSONObject json = new JSONObject();
				json.put(Key.ACTION.getName(), Action.UDP_SESSION_PUSH.getName());
				json.put(Key.DATA.getName(), list.subList(i, Math.min(i + batch, list.size())));
				pushHandler.push(json.toString());
			}
		};
	}
}
