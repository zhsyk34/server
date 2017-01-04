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
 * put the same request target in the queue
 * and wait to process one by one
 */
@NoArgsConstructor(staticName = "instance")
@Getter
@Setter
final class AppRequestQueue {
	private final BlockingQueue<AppRequest> queue = new LinkedBlockingQueue<>();
	private volatile boolean send = false;
	private volatile long time = -1;

	/**
	 * reset the queue status
	 */
	private synchronized AppRequestQueue reset() {
		this.send = false;
		this.time = -1;
		return this;
	}

	/**
	 * when begin to process the message queue,guard it
	 */
	private synchronized AppRequestQueue guard() {
		this.send = true;
		this.time = System.currentTimeMillis();
		return this;
	}

	/**
	 * offer the new message
	 */
	boolean offer(AppRequest request) {
		return request != null && queue.offer(request);
	}

	/**
	 * when process the message
	 * check the status
	 * if not send yet try to peek the first one and guard it
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
	 * when the first message have done,remove it and reset the status
	 * before this check the send status
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
	 * clear all queue and reset status
	 * and return clone queue for process after
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
