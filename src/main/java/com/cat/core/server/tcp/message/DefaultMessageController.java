package com.cat.core.server.tcp.message;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.ErrNo;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import com.cat.core.server.task.LoopTask;
import com.cat.core.server.tcp.session.SessionController;
import com.cat.core.server.web.PushController;
import io.netty.channel.Channel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultMessageController implements MessageController {
	private static final Map<String, AppRequestQueue> APP_REQUESTS_MAP = new ConcurrentHashMap<>();
	@NonNull
	private final PushController pushController;
	@NonNull
	private final SessionController sessionController;

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
	public boolean push(@NonNull String msg) {
		return pushController.push(msg);
	}

	@Override
	public LoopTask process() {
		return () -> APP_REQUESTS_MAP.forEach((sn, queue) -> {
			AppRequest request = queue.peek();
			if (request != null) {
				this.forward(sn, request.getMessage());
			}
		});
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
	public LoopTask monitor() {
		return () -> {
			AtomicInteger count = new AtomicInteger();
			APP_REQUESTS_MAP.forEach((sn, queue) -> count.addAndGet(queue.getQueue().size()));
			Log.logger(Factory.TCP_EVENT, "共有条[" + count.get() + "]信息待处理");
			System.err.println("共有条[" + count.get() + "]信息待处理");

			APP_REQUESTS_MAP.forEach((sn, queue) -> {
				if (queue.isSend() && !ValidateKit.time(queue.getTime(), Config.TCP_MESSAGE_TIMEOUT)) {
					sessionController.close(sn, Device.GATEWAY);
					Queue<AppRequest> history = queue.clear();
					if (history != null) {
						Log.logger(Factory.TCP_ERROR, "网关[" + sn + "]响应超时,关闭连接并移除当前所有请求,共[" + history.size() + "]条");
						feedback(history);
					}
				}
			});
		};
	}

	/**
	 * forward message to gateway
	 */
	private boolean forward(String sn, String msg) {
		Log.logger(Factory.TCP_EVENT, "向网关[" + sn + "]转发app请求[" + msg + "]");
		Channel channel = sessionController.channel(sn, Device.GATEWAY);
		if (channel == null) {
			sessionController.awake(sn);
		}
		channel = sessionController.channel(sn, Device.GATEWAY);
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
		Channel channel = sessionController.channel(id, Device.APP);
		if (channel == null) {
			Log.logger(Factory.TCP_EVENT, "客户端[" + id + "]已下线");
			return false;
		}
		channel.writeAndFlush(msg);
		Log.logger(Factory.TCP_EVENT, "响应客户端[" + id + "]的请求");
		return true;
	}

	/**
	 * feedback app client when gateway process timeout in the thread async
	 */
	private void feedback(Queue<AppRequest> queue) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.NO.getName());
		json.put(Key.ERROR_NO.getName(), ErrNo.TIMEOUT.getCode());
		json.put(Key.ERROR_INFO.getName(), ErrNo.TIMEOUT.getDescription());
		service.submit(() -> queue.forEach(message -> this.reply(message.getId(), json.toString())));
		service.shutdown();
	}

}
