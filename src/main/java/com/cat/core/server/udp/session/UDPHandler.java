package com.cat.core.server.udp.session;

import com.cat.core.server.task.LoopTask;
import lombok.NonNull;

import java.net.InetSocketAddress;

public interface UDPHandler {

	UDPInfo find(@NonNull String sn);

	void receive(@NonNull UDPInfo info);

	/**
	 * response udp heart beat
	 */
	void response(@NonNull InetSocketAddress target);

	/**
	 * send message to awake gateway login
	 */
	void awake(@NonNull InetSocketAddress target);

	/**
	 * clean stale data
	 */
	void clean();

	/**
	 * push session data to web server
	 */
	LoopTask push();
}
