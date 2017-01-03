package com.cat.core.server.tcp.session;

import com.cat.core.server.dict.Device;
import io.netty.channel.Channel;
import lombok.NonNull;

public interface SessionHandler {
	/**
	 * get channel
	 */
	Channel channel(@NonNull String key, Device device);

	/**
	 * active at once
	 */
	void active(@NonNull Channel channel);

	/**
	 * awake un login gateway client
	 */
	boolean awake(@NonNull String sn);

	/**
	 * allocate udp port if necessary
	 */
	int assign(@NonNull Channel channel);

	/**
	 * haven pass the verify
	 */
	void register(@NonNull Channel channel);

	/**
	 * close channel and release
	 */
	boolean unRegister(@NonNull Channel channel);

	/**
	 * close channel by key with assigned type
	 */
	boolean close(@NonNull String key, Device device);

	/**
	 * monitor resource
	 */
	void monitor(Device device);

}
