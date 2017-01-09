package com.cat.core.server.tcp.session;

import com.cat.core.config.Config;
import com.cat.core.kit.ThreadKit;
import com.cat.core.kit.ValidateKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.Device;
import com.cat.core.server.task.LoopTask;
import com.cat.core.server.tcp.port.PortController;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.LoginInfo;
import com.cat.core.server.udp.session.UDPController;
import com.cat.core.server.udp.session.UDPInfo;
import com.cat.core.server.web.PushController;
import io.netty.channel.Channel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultSessionController implements SessionController {
	private static final Map<String, Channel> ACCEPT_MAP = new ConcurrentHashMap<>();
	private static final Map<String, Channel> APP_MAP = new ConcurrentHashMap<>(Config.TCP_APP_COUNT_PREDICT);
	private static final Map<String, Channel> GATEWAY_MAP = new ConcurrentHashMap<>(Config.TCP_GATEWAY_COUNT_PREDICT);
	@NonNull
	private final PortController portController;
	@NonNull
	private final UDPController udpController;
	@NonNull
	private final PushController pushController;

	@Override
	public void active(@NonNull Channel channel) {
		Log.logger(Factory.TCP_EVENT, "[" + ChannelData.ip(channel) + ":" + ChannelData.port(channel) + "] 连接成功");
		ACCEPT_MAP.put(ChannelData.id(channel), channel);
	}

	@Override
	public boolean awake(@NonNull String sn) {
		Channel channel = GATEWAY_MAP.get(sn);
		if (channel != null) {
			Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]已登录,无需唤醒");
			return true;
		}

		UDPInfo info = udpController.find(sn);
		if (info == null) {
			Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]掉线(无UDP心跳),无法唤醒");
			return false;
		}

		String ip = info.getIp();
		int port = info.getPort();//heart beat port

		int chance = 0;//try times

		while (chance < 3 && !GATEWAY_MAP.containsKey(sn)) {
			chance++;

			udpController.awake(new InetSocketAddress(ip, port));
			ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);

			if (GATEWAY_MAP.containsKey(sn)) {
				return true;
			}

			int allocated = portController.port(ip, sn);//tcp allocate port
			if (allocated != port) {
				udpController.awake(new InetSocketAddress(ip, allocated));
				ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);
			}
		}

		return GATEWAY_MAP.containsKey(sn);
	}

	@Override
	public int assign(@NonNull Channel channel) {
		LoginInfo info = ChannelData.info(channel);

		int allocated;
		switch (info.getDevice()) {
			case APP:
				allocated = 0;
				break;
			case GATEWAY:
				allocated = portController.allocate(info.getSn(), ChannelData.ip(channel), info.getApply());
				break;
			default:
				allocated = -1;
				break;
		}

		info.setApply(allocated);

		return allocated;
	}

	@Override
	public void register(@NonNull Channel channel) {
		String id = ChannelData.id(channel);

		if (ACCEPT_MAP.remove(id) == null) {
			Log.logger(Factory.TCP_EVENT, "[" + ChannelData.info(channel).getSn() + "]登录超时,登录失败");
			return;
		}

		Channel original;
		LoginInfo info = ChannelData.info(channel);
		switch (info.getDevice()) {
			case APP:
				original = APP_MAP.put(id, channel);
				Log.logger(Factory.TCP_EVENT, "app[" + id + "]登录成功");
				break;
			case GATEWAY:
				pushController.push(GatewayInfo.login(channel));

				original = GATEWAY_MAP.put(info.getSn(), channel);
				Log.logger(Factory.TCP_EVENT, "网关[" + info.getSn() + "]登录成功");
				break;
			default:
				original = null;
				break;
		}

		if (original != null) {
			Log.logger(Factory.TCP_EVENT, "关闭原有的连接[" + ChannelData.info(original).getHappen() + "]");
			original.close();
		}
	}

	@Override
	public boolean unRegister(@NonNull Channel channel) {
		String id = ChannelData.id(channel);
		if (ACCEPT_MAP.remove(id, channel)) {
			return true;
		}

		LoginInfo info = ChannelData.info(channel);
		if (info == null) {
			//normally it won't happen
			return false;
		}

		Device device = info.getDevice();
		if (device == null) {
			//normally it won't happen
			return false;
		}

		//the channel has enter into login
		switch (device) {
			case APP:
				if (APP_MAP.remove(id, channel)) {
					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + "客户端关闭出错,在APP队列中查找不到该连接(可能因为线时长已到被移除)");
				return false;
			case GATEWAY:
				//different with close(key)
				if (GATEWAY_MAP.remove(info.getSn(), channel)) {
					pushController.push(GatewayInfo.logout(channel));

					Log.logger(Factory.TCP_EVENT, "网关[" + info.getSn() + "]下线");
					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + " 网关关闭出错,在网关队列中查找不到该连接(可能因在线时长已到被移除或重新登录时被关闭)");
				return false;
			default:
				Log.logger(Factory.TCP_ERROR, "关闭出错,非法的登录数据");
				return false;
		}
	}

	@Override
	public boolean close(@NonNull String key, Device device) {
		Channel channel = null;

		if (device == null) {
			channel = ACCEPT_MAP.remove(key);
		} else {
			switch (device) {
				case APP:
					channel = APP_MAP.remove(key);
					break;
				case GATEWAY:
					channel = GATEWAY_MAP.remove(key);
					if (channel != null) {
						pushController.push(GatewayInfo.logout(channel));
					}
					break;
				default:
					break;
			}
		}

		if (channel != null) {
			channel.close();
			return true;
		}
		return false;
	}

	@Override
	public Channel channel(@NonNull String key, Device device) {
		if (device == null) {
			return ACCEPT_MAP.get(key);
		}
		switch (device) {
			case APP:
				return APP_MAP.get(key);
			case GATEWAY:
				return GATEWAY_MAP.get(key);
			default:
				return null;
		}
	}

	@Override
	public List<LoopTask> monitor() {
		LoopTask acceptTask = () -> {
			Log.logger(Factory.TCP_EVENT, "未登录连接数:[" + ACCEPT_MAP.size() + "]");
			ACCEPT_MAP.forEach((id, channel) -> {
				if (!ValidateKit.time(ChannelData.info(channel).getHappen(), Config.TCP_LOGIN_TIMEOUT)) {
					Log.logger(Factory.TCP_EVENT, "超时未登录,移除");
					if (ACCEPT_MAP.remove(id, channel)) {
						channel.close();
					}
				}
			});
		};

		LoopTask appTask = () -> {
			Log.logger(Factory.TCP_EVENT, "TCP APP在线:[" + APP_MAP.size() + "]");
			APP_MAP.forEach((id, channel) -> {
				if (!ValidateKit.time(ChannelData.info(channel).getHappen(), Config.TCP_APP_TIMEOUT)) {
					Log.logger(Factory.TCP_EVENT, "APP在线时长已到,移除!");
					if (APP_MAP.remove(id, channel)) {
						channel.close();
					}
				}
			});
		};

		LoopTask gatewayTask = () -> {
			Log.logger(Factory.TCP_EVENT, "TCP网关在线:[" + GATEWAY_MAP.size() + "]");
			GATEWAY_MAP.forEach((id, channel) -> {
				if (!ValidateKit.time(ChannelData.info(channel).getHappen(), Config.TCP_GATEWAY_TIMEOUT)) {
					Log.logger(Factory.TCP_EVENT, "APP在线时长已到,移除!");
					if (GATEWAY_MAP.remove(id, channel)) {
						channel.close();
					}
				}
			});
		};

		return Collections.unmodifiableList(Arrays.asList(acceptTask, appTask, gatewayTask));
	}

}
