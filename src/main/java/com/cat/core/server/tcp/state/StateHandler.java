package com.cat.core.server.tcp.state;

import io.netty.channel.Channel;
import lombok.NonNull;

/**
 * validate state for event to session
 * and process after each state change
 */
public interface StateHandler {

	void create(@NonNull Channel channel);

	void onCreate(@NonNull Channel channel);

	void request(@NonNull Channel channel, @NonNull LoginInfo info);

	void onRequest(@NonNull Channel channel);

	void ready(@NonNull Channel channel);

	void onReady(@NonNull Channel channel, String question);

	void verify(@NonNull Channel channel, @NonNull String answer);

	void onVerify(@NonNull Channel channel, boolean result);

	void pass(@NonNull Channel channel);

	void onPass(@NonNull Channel channel);

	void wait(@NonNull Channel channel);

	void onWait(@NonNull Channel channel);

	void success(@NonNull Channel channel, int allocated);

	void onSuccess(@NonNull Channel channel);

	void close(@NonNull Channel channel);

	void onClose(@NonNull Channel channel);
}