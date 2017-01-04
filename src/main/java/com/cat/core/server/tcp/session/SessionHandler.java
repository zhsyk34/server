package com.cat.core.server.tcp.session;

import com.cat.core.server.data.Device;
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
	 * awake un-login gateway client
	 */
	boolean awake(@NonNull String sn);

	/**
	 * allocate udp port2 if necessary(just for gateway)
	 */
	int assign(@NonNull Channel channel);

	/**
	 * for login success
	 */
	void register(@NonNull Channel channel);

	/**
	 * close channel and release the resources
	 */
	boolean unRegister(@NonNull Channel channel);

	/**
	 * close channel by key with assigned type
	 * it possible close the channel who login later
	 */
	boolean close(@NonNull String key, Device device);

	/**
	 * monitor register channel
	 */
	void monitor();

}
