package com.cat.core.server.tcp.message;

import lombok.NonNull;

public interface MessageHandler {

	boolean receive(@NonNull String sn, @NonNull AppRequest request);

	/**
	 * push to the web service for message received from the gateway push
	 */
	boolean push(@NonNull String msg);

	void process();

	boolean response(@NonNull String sn, @NonNull String msg);

	/**
	 * TODO:TEST
	 * monitor the process situation
	 */
	void monitor();

}
