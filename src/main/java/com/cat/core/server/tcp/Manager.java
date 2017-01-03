package com.cat.core.server.tcp;

import com.cat.core.server.dict.State;
import com.cat.core.server.tcp.message.DefaultMessageHandler;
import com.cat.core.server.tcp.message.MessageHandler;
import com.cat.core.server.tcp.session.DefaultSessionHandler;
import com.cat.core.server.tcp.session.SessionHandler;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.DefaultStateHandler;
import com.cat.core.server.tcp.state.LoginInfo;
import com.cat.core.server.tcp.state.StateHandler;
import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(staticName = "instance")
public final class Manager {

	private final SessionHandler eventHandler = DefaultSessionHandler.instance();
	private final StateHandler stateHandler = DefaultStateHandler.instance(eventHandler);
	private final MessageHandler messageHandler = DefaultMessageHandler.instance(eventHandler);

	public final void create(@NonNull Channel channel) {
		stateHandler.create(channel);
	}

	public final void request(@NonNull Channel channel, @NonNull LoginInfo info) {
		stateHandler.request(channel, info);
	}

	public final void verify(@NonNull Channel channel, @NonNull String answer) {
		stateHandler.verify(channel, answer);
	}

	public final void close(@NonNull Channel channel) {
		stateHandler.close(channel);
	}

	public final boolean finished(@NonNull Channel channel) {
		return ChannelData.state(channel) == State.SUCCESS;
	}

	public final void receive(@NonNull String sn, @NonNull String msg) {
		messageHandler.receive(sn, msg);
	}
}
