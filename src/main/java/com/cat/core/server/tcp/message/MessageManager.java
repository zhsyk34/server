package com.cat.core.server.tcp.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.ErrNo;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.tcp.session.SessionManager;
import io.netty.channel.Channel;
import lombok.NonNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP信息管理
 */
public final class MessageManager {

	/**
	 * APP请求消息处理队列
	 */
	private static final Map<String, AppRequestQueue> APP_REQUESTS_MAP = new ConcurrentHashMap<>();

	/**
	 * 将APP请求添加到消息处理队列
	 */
	public static boolean register(String sn, AppRequest msg) {
		final AppRequestQueue queue;
		synchronized (APP_REQUESTS_MAP) {
			if (APP_REQUESTS_MAP.containsKey(sn)) {
				queue = APP_REQUESTS_MAP.get(sn);
			} else {
				queue = AppRequestQueue.instance();
				APP_REQUESTS_MAP.put(sn, queue);
			}
		}
		return queue.offer(msg);
	}

	/**
	 * 网关响应app请求后将其从相应的处理队列中移除
	 */
	public static void response(String sn, String msg) {
		AppRequestQueue queue = APP_REQUESTS_MAP.get(sn);

		AppRequest message = queue.poll();
		if (message != null) {
			SessionManager.response(message.getId(), msg);
		}
	}

	/**
	 * TODO judge by the info in the channel
	 * 将网关上线信息推送至WEB服务器
	 */
	public static void push(@NonNull Channel channel, boolean online) {
//		JSONObject json = (JSONObject) JSON.toJSON(info);
//		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());
//		UDPPusher.push(json);
	}

	/**
	 * 将网关TCP推送信息通过UDP推送至WEB服务器
	 */
	public static void push(@NonNull String sn, @NonNull String msg) {
		JSONObject json = JSON.parseObject(msg);
		json.put(Key.SN.getName(), sn);
		UDPPusher.push(json);
	}
//
//	/**
//	 * 将网关上线信息推送至WEB服务器
//	 */
//	public static void loginPush(@NonNull TCPInfo info) {
//		JSONObject json = (JSONObject) JSON.toJSON(info);
//		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());
//		UDPPusher.push(json);
//	}

//	/**
//	 * 将网关下线信息推送至WEB服务器
//	 */
//	public static void logoutPush(@NonNull String sn) {
//		System.err.println(sn + "下线...");
//		JSONObject json = new JSONObject();
//		json.put(Key.ACTION.getName(), Action.TCP_LOGOUT_PUSH.getName());
//		json.put("sn", sn);
//		json.put("happen", System.currentTimeMillis());
//		UDPPusher.push(json);
//	}

	/**
	 * 处理app消息队列
	 */
	public static void process() {
		AtomicInteger count = new AtomicInteger();

		APP_REQUESTS_MAP.forEach((sn, queue) -> {
			count.addAndGet(queue.getQueue().size());
			AppRequest message = queue.peek();
			if (message != null) {
				SessionManager.forward(sn, message.getMessage());
			}
		});
	}

	/**
	 * 网关响应超时则清空当前的请求队列信息并关闭网关连接
	 * 同时提示app
	 */
	@SuppressWarnings("InfiniteLoopStatement")
	public static void monitor() {
		while (true) {
			APP_REQUESTS_MAP.forEach((sn, queue) -> {
				if (queue.isSend() && !ValidateKit.time(queue.getTime(), Config.TCP_MESSAGE_HANDLE_TIMEOUT)) {
					SessionManager.unRegister(sn);
					Queue<AppRequest> history = queue.clear();
					if (history != null) {
						Log.logger(Factory.TCP_ERROR, "网关[" + sn + "]响应超时,关闭连接并移除当前所有请求,共[" + history.size() + "]条");
						feedback(history);
					}
				}
			});
		}
	}

	/**
	 * 回馈响应失败
	 *
	 * @param queue 需要回馈的消息队列
	 */
	private static void feedback(Queue<AppRequest> queue) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.NO.getName());
		json.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
		json.put(Key.ERROR_INFO.getName(), ErrNo.TIMEOUT.getDescription());
		service.submit(() -> queue.forEach(message -> SessionManager.response(message.getId(), json.toString())));
		service.shutdown();
	}

}
