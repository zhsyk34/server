package com.cat.core.server;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.State;
import com.cat.core.server.task.LoopTask;
import com.cat.core.server.task.TaskHandler;
import com.cat.core.server.task.TimerTask;
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
import com.cat.core.server.udp.session.DefaultUDPHandler;
import com.cat.core.server.udp.session.UDPHandler;
import com.cat.core.server.udp.session.UDPInfo;
import com.cat.core.server.web.PushHandler;
import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * the center controller for server
 */
@NoArgsConstructor(staticName = "instance")
public final class Controller {

	/**
	 * will load data from db
	 */
	private final PortHandler portHandler = DefaultPortHandler.instance();

	/**
	 * will start udp client for push message to web server
	 */
	private final PushHandler pushHandler = PushHandler.instance();

	private final UDPHandler udpHandler = DefaultUDPHandler.instance(pushHandler);

	private final SessionHandler sessionHandler = DefaultSessionHandler.instance(portHandler, udpHandler, pushHandler);

	private final StateHandler stateHandler = DefaultStateHandler.instance(sessionHandler);

	private final MessageHandler messageHandler = DefaultMessageHandler.instance(pushHandler, sessionHandler);

	private final TaskHandler taskHandler = TaskHandler.instance();

	/*tcp*/
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

	public final boolean push(@NonNull String msg) {
		return messageHandler.push(msg);
	}

	public final boolean push(@NonNull JSONObject json) {
		return this.push(json.toString());
	}

	public final void receive(@NonNull String sn, @NonNull AppRequest request) {
		messageHandler.receive(sn, request);
	}

	public final void response(@NonNull String sn, @NonNull String msg) {
		messageHandler.response(sn, msg);
	}

	/*udp*/
	public final void receive(@NonNull UDPInfo info) {
		Log.logger(Factory.UDP_RECEIVE, info.toString());
		udpHandler.receive(info);
	}

	public final void response(@NonNull InetSocketAddress target) {
		udpHandler.response(target);
	}

	public final void task() {
		//loop tasks
		List<LoopTask> loopTasks = new ArrayList<>();

		loopTasks.addAll(sessionHandler.monitor());
		loopTasks.add(messageHandler.monitor());
		loopTasks.add(messageHandler.process());

		taskHandler.register(loopTasks.toArray(new LoopTask[loopTasks.size()]));

		//timer tasks
		List<TimerTask> timerTasks = new ArrayList<>();
		timerTasks.add(portHandler.recycle());
		taskHandler.register(timerTasks.toArray(new TimerTask[timerTasks.size()]));

		taskHandler.execute();
	}

}
