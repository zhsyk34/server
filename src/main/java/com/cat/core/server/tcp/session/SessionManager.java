package com.cat.core.server.tcp.session;

import com.cat.core.config.Config;
import com.cat.core.kit.ThreadKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.dict.Device;
import com.cat.core.server.tcp.message.MessageManager;
import com.cat.core.server.udp.session.UDPInfo;
import com.cat.core.server.udp.session.UDPManager;
import io.netty.channel.Channel;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP会话(连接)管理
 */
public final class SessionManager {

	private static final DefaultEventHandler handler = DefaultEventHandler.instance();

	/**
	 * 请求连接(登录后移除)
	 * 一连接即记录,开启线程扫描以关闭超时未登录的连接,故此在登录验证中可不验证登录时间
	 */
	private static final Map<String, Channel> ACCEPT_MAP = new ConcurrentHashMap<>();

	/**
	 * 登录的app连接,key为channel默认id
	 */
	private static final Map<String, Channel> APP_MAP = new ConcurrentHashMap<>(Config.TCP_APP_COUNT_PREDICT);

	/**
	 * 登录的网关连接,key为网关sn号
	 */
	private static final Map<String, Channel> GATEWAY_MAP = new ConcurrentHashMap<>(Config.TCP_GATEWAY_COUNT_PREDICT);

	/* ----------------------------------------连接信息----------------------------------------*/

	private static String ip(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

//	private static int port(@NonNull Channel channel) {
//		return ((InetSocketAddress) channel.remoteAddress()).getPort();
//	}

	public static String id(@NonNull Channel channel) {
		return channel.id().asLongText();
	}


	/* ----------------------------------------会话管理----------------------------------------*/

	/**
	 * init and register in to accept context
	 */
	static void init(Channel channel) {
		ACCEPT_MAP.put(id(channel), channel);
	}

	/**
	 * remove record from init at accept
	 * allocate udp port for gateway and default 0 for app
	 * if wrong return -1
	 */
	static int allocate(@NonNull Channel channel) {
		if (ACCEPT_MAP.remove(id(channel)) == null) {
			//正常情况下此连接已被关闭
			Log.logger(Factory.TCP_EVENT, "登录超时(未及时登录,会话已关闭)");
			return -1;
		}

		LoginInfo info = handler.info(channel);

		int allocated;
		switch (info.getDevice()) {
			case APP:
				allocated = 0;
				break;
			case GATEWAY:
				allocated = PortManager.allocate(info.getSn(), ip(channel), info.getApply());
				break;
			default:
				allocated = -1;
				break;
		}

		info.setApply(allocated);

		return allocated;
	}

	/**
	 * TODO UDP推送
	 * when login success register to context
	 */
	static void register(@NonNull Channel channel) {
		LoginInfo info = handler.info(channel);

		Channel original;
		switch (info.getDevice()) {
			case APP:
				original = APP_MAP.put(id(channel), channel);
				break;
			case GATEWAY:
				/*push*/
				MessageManager.push(channel, true);

				original = GATEWAY_MAP.put(info.getSn(), channel);
				break;
			default:
				original = null;
				break;
		}

		if (original != null) {
			original.close();
		}
	}

	/**
	 * 根据sn关闭网关连接
	 * 存在网关重新登录后被关闭的可能
	 */
	public static void unRegister(String sn) {
		Channel channel = GATEWAY_MAP.remove(sn);
		if (channel != null) {
			channel.close();
		}

		/*push*/
		MessageManager.push(channel, false);
	}

	/**
	 * 关闭连接并将其从相应的队列中移除
	 */
	static boolean unRegister(@NonNull Channel channel) {
		channel.close();

		if (ACCEPT_MAP.remove(id(channel), channel)) {
			return true;
		}

		LoginInfo info = DefaultEventHandler.instance().info(channel);
		if (info == null) {
			//normally it won't happen
			return false;
		}

		Device device = info.getDevice();

		//已进入登录环节
		switch (device) {
			case APP:
				if (APP_MAP.remove(id(channel), channel)) {
					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + "客户端关闭出错,在app队列中查找不到该连接(可能在线时长已到被移除)");
				return false;
			case GATEWAY:
				if (GATEWAY_MAP.remove(info.getSn(), channel)) {
					/*push*/
					MessageManager.push(channel, false);

					return true;
				}
				Log.logger(Factory.TCP_ERROR, channel.remoteAddress() + " 网关关闭出错,在网关队列中查找不到该连接(可能在线时长已到被移除)");
				return false;
			default:
				Log.logger(Factory.TCP_ERROR, "关闭出错,非法的登录数据");
				return false;
		}
	}

	/**
	 * 尝试唤醒网关
	 */
	private static boolean awake(String sn) {
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

			int allocated = PortManager.port(ip, sn);//服务器分配端口
			if (allocated != port) {
				UDPManager.awake(ip, allocated);
				ThreadKit.await(Config.GATEWAY_AWAKE_CHECK_TIME);
			}
		}

		return GATEWAY_MAP.containsKey(sn);
	}

	 /* ----------------------------------------TCP消息请求处理----------------------------------------*/

	/**
	 * 转发客户端请求至网关
	 *
	 * @param sn  网关sn
	 * @param msg 客户端的请求指令
	 */
	public static boolean forward(String sn, String msg) {
		Log.logger(Factory.TCP_EVENT, "向网关[" + sn + "]转发app请求[" + msg + "]");
		Channel channel = GATEWAY_MAP.get(sn);
		if (channel == null) {
			awake(sn);
		}
		channel = GATEWAY_MAP.get(sn);
		if (channel == null) {
			Log.logger(Factory.TCP_EVENT, "唤醒网关[" + sn + "]失败,无法转发app请求");
			return false;
		}
		channel.writeAndFlush(msg);
		Log.logger(Factory.TCP_EVENT, "已转发[" + msg + "] ==> 网关[" + sn + "]");
		return true;
	}

	/**
	 * @param id  客户端连接标识
	 * @param msg 网关对客户端请求的响应
	 */
	public static boolean response(String id, String msg) {
		Channel channel = APP_MAP.get(id);
		if (channel == null) {
			Log.logger(Factory.TCP_EVENT, "客户端[" + id + "]已下线");
			return false;
		}
		channel.writeAndFlush(msg);
		Log.logger(Factory.TCP_EVENT, "响应客户端[" + id + "]的请求");
		return true;
	}

	/**
	 * TODO
	 */
	public static void monitor() {
		System.err.println("gateway count:[" + GATEWAY_MAP.size() + "]");
		GATEWAY_MAP.forEach((sn, channel) -> System.err.println(handler.info(channel)));
	}
}
