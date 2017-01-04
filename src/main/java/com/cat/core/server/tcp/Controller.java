package com.cat.core.server.tcp;

import com.cat.core.server.data.State;
import com.cat.core.server.tcp.message.AppRequest;
import com.cat.core.server.tcp.message.DefaultMessageHandler;
import com.cat.core.server.tcp.message.MessageHandler;
import com.cat.core.server.tcp.port.DefaultPortHandler;
import com.cat.core.server.tcp.port.PortHandler;
import com.cat.core.server.tcp.session.DefaultSessionHandler;
import com.cat.core.server.tcp.session.SessionHandler;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.DefaultStateHandler;
import com.cat.core.server.tcp.state.LoginInfo;
import com.cat.core.server.tcp.state.StateHandler;
import com.cat.core.server.web.PushHandler;
import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * the center controller for tcp server
 */
@NoArgsConstructor(staticName = "instance")
final class Controller {

	private final PushHandler pushHandler = PushHandler.instance();

	private final PortHandler portHandler = DefaultPortHandler.instance();

	private final SessionHandler sessionHandler = DefaultSessionHandler.instance(portHandler, pushHandler);

	private final StateHandler stateHandler = DefaultStateHandler.instance(sessionHandler);

	private final MessageHandler messageHandler = DefaultMessageHandler.instance(pushHandler, sessionHandler);

	final void create(@NonNull Channel channel) {
		stateHandler.create(channel);
	}

	final void request(@NonNull Channel channel, @NonNull LoginInfo info) {
		stateHandler.request(channel, info);
	}

	final void verify(@NonNull Channel channel, @NonNull String answer) {
		stateHandler.verify(channel, answer);
	}

	final void close(@NonNull Channel channel) {
		stateHandler.close(channel);
	}

	final boolean finished(@NonNull Channel channel) {
		return ChannelData.state(channel) == State.SUCCESS;
	}

	final boolean push(@NonNull String msg) {
		return messageHandler.push(msg);
	}

	final void receive(@NonNull String sn, @NonNull AppRequest request) {
		messageHandler.receive(sn, request);
	}

	final void response(@NonNull String sn, @NonNull String msg) {
		messageHandler.response(sn, msg);
	}

	final void process() {
		messageHandler.process();
	}

	final void monitor() {
		messageHandler.monitor();
		sessionHandler.monitor();
	}
}
