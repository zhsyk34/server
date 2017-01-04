package com.cat.core.server.tcp.message;

import com.cat.core.server.task.LoopTask;
import lombok.NonNull;

public interface MessageHandler {

	boolean receive(@NonNull String sn, @NonNull AppRequest request);

	/**
	 * push to the web service for message received from the gateway push
	 */
	boolean push(@NonNull String msg);

	LoopTask process();

	boolean response(@NonNull String sn, @NonNull String msg);

	/**
	 * monitor the process situation
	 */
	LoopTask monitor();

}
