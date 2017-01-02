package com.cat.core.server.tcp.session;

import io.netty.channel.Channel;
import lombok.NonNull;

/**
 * validate state for event to session
 * and replay after each state change event
 */
interface EventHandler {

	void create(@NonNull Channel channel);

	void request(@NonNull Channel channel, @NonNull LoginInfo info);

	void ready(@NonNull Channel channel);

	void verify(@NonNull Channel channel, @NonNull String offer);

	void pass(@NonNull Channel channel);

	void wait(@NonNull Channel channel);

	void success(@NonNull Channel channel, int allocated);

	void close(@NonNull Channel channel);
}