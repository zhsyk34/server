package com.cat.core.server.tcp.message;

import com.cat.core.config.Config;
import com.cat.core.kit.ValidateKit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 等待网关处理的app消息队列
 */
@NoArgsConstructor(staticName = "instance")
@Getter
@Setter
final class AppRequestQueue {
	private final BlockingQueue<AppRequest> queue = new LinkedBlockingQueue<>();
	private volatile boolean send = false;
	private volatile long time = -1;

	/**
	 * 重置队列状态
	 */
	private synchronized AppRequestQueue reset() {
		this.send = false;
		this.time = -1;
		return this;
	}

	/**
	 * 当队列数据被处理时开启警戒状态以进行监测
	 */
	private synchronized AppRequestQueue guard() {
		this.send = true;
		this.time = System.currentTimeMillis();
		return this;
	}

	/**
	 * 添加数据
	 */
	boolean offer(AppRequest request) {
		return request != null && queue.offer(request);
	}

	/**
	 * 处理队首元素,先查看其是否正被处理
	 * 如是则不进行任何操作,否则取出并进入警戒状态
	 */
	synchronized AppRequest peek() {
		if (send) {
			return null;
		}

		AppRequest request = queue.peek();
		if (request != null) {
			this.guard();
		}
		return request;
	}

	/**
	 * 移除已处理完的数据并重置状态
	 */
	synchronized AppRequest poll() {
		if (!send) {
			return null;
		}

		AppRequest request = queue.poll();
		if (request != null) {
			this.reset();
		}
		return request;
	}

	/**
	 * 清空队列并返回当前队列中所有元素的副本
	 */
	synchronized Queue<AppRequest> clear() {
		if (send && !ValidateKit.time(time, Config.TCP_MESSAGE_HANDLE_TIMEOUT)) {
			BlockingQueue<AppRequest> copy = new LinkedBlockingQueue<>(queue);
			queue.clear();
			this.reset();
			return copy;
		}
		return null;
	}
}
