package com.cat.core.server.tcp.message;

import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.Device;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultMessageHandler implements MessageHandler {

	private static final Map<String, AppRequestQueue> APP_REQUESTS_MAP = new ConcurrentHashMap<>();

	@Override
	public boolean receive(@NonNull String sn, @NonNull AppRequest request) {
		final AppRequestQueue queue;
		synchronized (APP_REQUESTS_MAP) {
			if (APP_REQUESTS_MAP.containsKey(sn)) {
				queue = APP_REQUESTS_MAP.get(sn);
			} else {
				queue = AppRequestQueue.instance();
				APP_REQUESTS_MAP.put(sn, queue);
			}
		}
		return queue.offer(request);
	}

	@Override
	public void process() {

		//TODO
	}

	@Override
	public boolean response(@NonNull String sn, @NonNull String msg) {
		AppRequestQueue queue = APP_REQUESTS_MAP.get(sn);

		AppRequest message = queue.poll();
		if (message == null) {
			return false;
		}

		this.reply(message.getId(), msg);
		return true;
	}

	@Override
	public boolean push(@NonNull String sn, @NonNull String msg) {
		return false;
	}

	/**
	 * forward message to gateway
	 */
	private boolean forward(String sn, String msg) {
		Log.logger(Factory.TCP_EVENT, "向网关[" + sn + "]转发app请求[" + msg + "]");
		Channel channel = handler.channel(sn, Device.GATEWAY);
		if (channel == null) {
			handler.awake(sn);
		}
		channel = handler.channel(sn, Device.GATEWAY);
		if (channel == null) {
			Log.logger(Factory.TCP_EVENT, "唤醒网关[" + sn + "]失败,无法转发app请求");
			return false;
		}
		channel.writeAndFlush(msg);
		Log.logger(Factory.TCP_EVENT, "已转发[" + msg + "] ==> 网关[" + sn + "]");
		return true;
	}

	/**
	 * reply message to app
	 */
	private boolean reply(String id, String msg) {
		Channel channel = handler.channel(id, Device.APP);
		if (channel == null) {
			Log.logger(Factory.TCP_EVENT, "客户端[" + id + "]已下线");
			return false;
		}
		channel.writeAndFlush(msg);
		Log.logger(Factory.TCP_EVENT, "响应客户端[" + id + "]的请求");
		return true;
	}

//	private final SessionHandler handler;
//
//	//TODO 测试统计
//	public static void total() {
//		AtomicInteger count = new AtomicInteger(0);
//
//		APP_REQUESTS_MAP.forEach((sn, queue) -> {
//			count.addAndGet(queue.getQueue().size());
//		});
//
//		System.err.println("共[" + count.get() + "]条待处理数据");
//	}
//

//	}
//
//	/**
//	 * TODO judge by the info in the channel
//	 * 将网关上线信息推送至WEB服务器
//	 */
//	public void push(@NonNull Channel channel, boolean online) {
////		JSONObject json = (JSONObject) JSON.toJSON(info);
////		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());
////		UDPPusher.push(json);
//	}
////
////	/**
////	 * 将网关上线信息推送至WEB服务器
////	 */
////	public static void loginPush(@NonNull TCPInfo info) {
////		JSONObject json = (JSONObject) JSON.toJSON(info);
////		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());
////		UDPPusher.push(json);
////	}
//
////	/**
////	 * 将网关下线信息推送至WEB服务器
////	 */
////	public static void logoutPush(@NonNull String sn) {
////		System.err.println(sn + "下线...");
////		JSONObject json = new JSONObject();
////		json.put(Key.ACTION.getName(), Action.TCP_LOGOUT_PUSH.getName());
////		json.put("sn", sn);
////		json.put("happen", System.currentTimeMillis());
////		UDPPusher.push(json);
////	}
//
//	/**
//	 * 将网关TCP推送信息通过UDP推送至WEB服务器
//	 */
//	@Override
//	public void push(@NonNull String sn, @NonNull String msg) {
//		JSONObject json = JSON.parseObject(msg);
//		json.put(Key.SN.getName(), sn);
//		UDPPusher.push(json);
//	}
//
//	/**
//	 * 处理app消息队列
//	 */
//	@Override
//	public void process() {
//		APP_REQUESTS_MAP.forEach((sn, queue) -> {
//			AppRequest request = queue.peek();
//			if (request != null) {
//				this.forward(sn, request.getMessage());
//			}
//		});
//	}
//
//	/**
//	 * 网关响应超时则清空当前的请求队列信息并关闭网关连接
//	 * 同时提示app
//	 */
//	@SuppressWarnings("InfiniteLoopStatement")
//	public void monitor() {
//		while (true) {
//			APP_REQUESTS_MAP.forEach((sn, queue) -> {
//				if (queue.isSend() && !ValidateKit.time(queue.getTime(), Config.TCP_MESSAGE_HANDLE_TIMEOUT)) {
//					handler.close(sn, Device.GATEWAY);
//					Queue<AppRequest> history = queue.clear();
//					if (history != null) {
//						Log.logger(Factory.TCP_ERROR, "网关[" + sn + "]响应超时,关闭连接并移除当前所有请求,共[" + history.size() + "]条");
//						feedback(history);
//					}
//				}
//			});
//		}
//	}
//
//	/**
//	 * 回馈响应失败
//	 *
//	 * @param queue 需要回馈的消息队列
//	 */
//	private void feedback(Queue<AppRequest> queue) {
//		ExecutorService service = Executors.newSingleThreadExecutor();
//		JSONObject json = new JSONObject();
//		json.put(Key.RESULT.getName(), Result.NO.getName());
//		json.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
//		json.put(Key.ERROR_INFO.getName(), ErrNo.TIMEOUT.getDescription());
//		service.submit(() -> queue.forEach(message -> this.response(message.getId(), json.toString())));
//		service.shutdown();
//	}

}
