package com.cat.core.server.tcp.session;

import com.cat.core.config.Config;
import com.cat.core.kit.ThreadKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.data.Device;
import com.cat.core.server.tcp.port.PortHandler;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.LoginInfo;
import com.cat.core.server.udp.session.UDPInfo;
import com.cat.core.server.udp.session.UDPManager;
import com.cat.core.server.web.PushHandler;
import io.netty.channel.Channel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultSessionHandler implements SessionHandler {

	private static final Map<String, Channel> ACCEPT_MAP = new ConcurrentHashMap<>();
	private static final Map<String, Channel> APP_MAP = new ConcurrentHashMap<>(Config.TCP_APP_COUNT_PREDICT);
	private static final Map<String, Channel> GATEWAY_MAP = new ConcurrentHashMap<>(Config.TCP_GATEWAY_COUNT_PREDICT);
	private final PortHandler portHandler;
	private final PushHandler pushHandler;

	@Override
	public void active(@NonNull Channel channel) {
		ACCEPT_MAP.put(ChannelData.id(channel), channel);
	}

	@Override
	public boolean awake(@NonNull String sn) {
		Channel channel = GATEWAY_MAP.get(sn);
		if (channel != null) {
			Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]已登录");
			return true;
		}

		UDPInfo info = UDPManager.find(sn);
		if (info == null) {
			Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]掉线(无udp心跳),无法唤醒");
			return false;
		}

		String ip = info.getIp();
		int port = info.getPort();//网关udp心跳端口

		int chance = 0;//尝试次数

		while (chance < 3 && !GATEWAY_MAP.containsKey(sn)) {
			chance++;

			UDPManager.awake(ip, port);
			ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);
			if (GATEWAY_MAP.containsKey(sn)) {
				return true;
			}

			int allocated = portHandler.port(ip, sn);//服务器分配端口
			if (allocated != port) {
				UDPManager.awake(ip, allocated);
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
				allocated = portHandler.allocate(info.getSn(), ChannelData.ip(channel), info.getApply());
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
			Log.logger(Factory.TCP_EVENT, "[" + ChannelData.info(channel).getSn() + "]登录超时");
			return;
		}

		Channel original;
		LoginInfo info = ChannelData.info(channel);
		switch (info.getDevice()) {
			case APP:
				original = APP_MAP.put(id, channel);
				Log.logger(Factory.TCP_EVENT, "app[" + id + "]上线");
				break;
			case GATEWAY:
				pushHandler.push(GatewayInfo.login(channel));

				original = GATEWAY_MAP.put(info.getSn(), channel);
				Log.logger(Factory.TCP_EVENT, "网关[" + info.getSn() + "]上线");
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

		//已进入登录环节
		switch (device) {
			case APP:
				if (APP_MAP.remove(id, channel)) {
					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + "客户端关闭出错,在app队列中查找不到该连接(可能在线时长已到被移除或网关重新登录关闭原有连接)");
				return false;
			case GATEWAY:
				//different with close(key)
				if (GATEWAY_MAP.remove(info.getSn(), channel)) {
					pushHandler.push(GatewayInfo.logout(channel));

					Log.logger(Factory.TCP_EVENT, "网关[" + info.getSn() + "]下线");
					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + " 网关关闭出错,在网关队列中查找不到该连接(可能在线时长已到被移除)");
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
						pushHandler.push(GatewayInfo.logout(channel));
					}
					break;
				default:
			}
		}

		if (channel != null) {
			channel.close();
			return true;
		}
		return false;
	}

	@Override
	public Channel channel(@NonNull String key, @NonNull Device device) {
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
	public void monitor() {
		//TODO
		System.err.println("accept count:[" + ACCEPT_MAP.size() + "]");
		System.err.println("gateway count:[" + GATEWAY_MAP.size() + "]");
		System.err.println("app count:[" + APP_MAP.size() + "]");
	}

}
