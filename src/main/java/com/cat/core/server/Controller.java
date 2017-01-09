package com.cat.core.server;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.State;
import com.cat.core.server.task.FixedTimerTask;
import com.cat.core.server.task.LoopTask;
import com.cat.core.server.task.TaskController;
import com.cat.core.server.tcp.message.AppRequest;
import com.cat.core.server.tcp.message.DefaultMessageController;
import com.cat.core.server.tcp.message.MessageController;
import com.cat.core.server.tcp.port.DefaultPortController;
import com.cat.core.server.tcp.port.PortController;
import com.cat.core.server.tcp.session.DefaultSessionController;
import com.cat.core.server.tcp.session.SessionController;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.DefaultStateController;
import com.cat.core.server.tcp.state.LoginInfo;
import com.cat.core.server.tcp.state.StateController;
import com.cat.core.server.udp.session.DefaultUDPController;
import com.cat.core.server.udp.session.UDPController;
import com.cat.core.server.udp.session.UDPInfo;
import com.cat.core.server.web.DefaultPushController;
import com.cat.core.server.web.PushController;
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
public class Controller {
	/**
	 * will load data from db
	 */
	private final PortController portController = DefaultPortController.instance();

	/**
	 * will start udp client for push message to web server
	 */
	private final PushController pushController = DefaultPushController.instance();

	private final UDPController udpController = DefaultUDPController.instance(pushController);

	private final SessionController sessionController = DefaultSessionController.instance(portController, udpController, pushController);

	private final StateController stateController = DefaultStateController.instance(sessionController);

	private final MessageController messageController = DefaultMessageController.instance(pushController, sessionController);

	private final TaskController taskController = TaskController.instance();

	/*-------------------------------------tcp connect event-------------------------------------*/
	public final void create(@NonNull Channel channel) {
		stateController.create(channel);
	}

	public final void request(@NonNull Channel channel, @NonNull LoginInfo info) {
		stateController.request(channel, info);
	}

	public final void verify(@NonNull Channel channel, @NonNull String answer) {
		stateController.verify(channel, answer);
	}

	public final void close(@NonNull Channel channel) {
		stateController.close(channel);
	}

	public final boolean finished(@NonNull Channel channel) {
		return ChannelData.state(channel) == State.SUCCESS;
	}

	/*-------------------------------------message-------------------------------------*/
	private boolean push(@NonNull String msg) {
		return messageController.push(msg);
	}

	public final boolean push(@NonNull JSONObject json) {
		return this.push(json.toString());
	}

	public final void receive(@NonNull String sn, @NonNull AppRequest request) {
		messageController.receive(sn, request);
	}

	public final void response(@NonNull String sn, @NonNull String msg) {
		messageController.response(sn, msg);
	}

	//response version request
	public final void version(@NonNull String sn, @NonNull JSONObject json) {
		Channel channel = sessionController.channel(sn, Device.GATEWAY);
		if (channel != null) {
			channel.writeAndFlush(json);
		}
	}

	/*-------------------------------------udp-------------------------------------*/
	public final void receive(@NonNull UDPInfo info) {
		udpController.receive(info);
	}

	public final void response(@NonNull InetSocketAddress target) {
		udpController.response(target);
	}

	public final void task() {
		/*---------------loop tasks---------------*/
		List<LoopTask> loopTasks = new ArrayList<>();
		//tcp connect monitor:连接超时监控
		loopTasks.addAll(sessionController.monitor());
		//message process monitor:消息处理超时监控
		loopTasks.add(messageController.monitor());
		//message process handler:消息处理
		loopTasks.add(messageController.process());
		//udp session push:推送udp心跳信息
		loopTasks.add(udpController.push());
		//udp session info clean:清理过期心跳数据
		loopTasks.add(udpController.clean());

		taskController.register(loopTasks.toArray(new LoopTask[loopTasks.size()]));

		/*---------------timer tasks---------------*/
		/*List<TimerTask> timerTasks = new ArrayList<>();

		taskController.register(timerTasks.toArray(new TimerTask[timerTasks.size()]));*/


		/*---------------fixed timer tasks---------------*/
		List<FixedTimerTask> fixedTimerTasks = new ArrayList<>();

		//tcp allocate udp port recycle:回收tcp服务器分配的udp端口
		fixedTimerTasks.add(portController.recycle());

		taskController.register(fixedTimerTasks.toArray(new FixedTimerTask[fixedTimerTasks.size()]));
		//执行
		taskController.execute();
	}
}
