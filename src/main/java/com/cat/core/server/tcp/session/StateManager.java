package com.cat.core.server.tcp.session;

import com.cat.core.config.Config;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.State;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.NonNull;

import java.net.InetSocketAddress;

/**
 * TCP会话状态(信息)管理
 */
public final class StateManager {

	//TODO
	private static final PortProvider PROVIDER = (sn, ip, apply) -> 0;

	/**
	 * 1.连接
	 */
	public static boolean create(@NonNull Channel channel) {
		TCPInfo info = TCPInfo.builder().happen(System.currentTimeMillis()).build();
		SessionData.info(channel, info);

		SessionData.state(channel, State.CREATE);
		return true;
	}

	/**
	 * 2.请求
	 */
	public static String request(@NonNull Channel channel, @NonNull TCPInfo info) {
		if (!SessionData.check(channel, State.CREATE) || info == null) {
			return null;
		}
		SessionData.state(channel, State.REQUEST);

		//update info
		SessionData.info(channel).setSn(info.getSn()).setDevice(info.getDevice()).setApply(info.getApply());
		//generator verifier
		Verifier verifier = Verifier.generator();
		SessionData.verifier(channel, verifier);

		SessionData.state(channel, State.READY);

		return SessionData.check(channel, State.READY) ? verifier.getQuestion() : null;
	}

	/**
	 * 校验
	 */
	public static boolean verify(@NonNull Channel channel, @NonNull String answer) {
		if (!SessionData.check(channel, State.READY)) {
			return false;
		}

		SessionData.state(channel, State.VERIFY);

		if (!SessionData.verifier(channel).getAnswer().equals(answer)) {
			return false;
		}
		SessionData.state(channel, State.PASS);
		return true;
	}

	/**
	 * 等待资源分配并通过
	 */
	public static int pass(@NonNull Channel channel) {
		if (!SessionData.check(channel, State.PASS)) {
			return -1;
		}
		SessionData.state(channel, State.WAIT);

		int allocated;
		TCPInfo info = SessionData.info(channel);
		switch (info.getDevice()) {
			case APP:
				info.setApply(allocated = 0);
				break;
			case GATEWAY:
				String ip = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
				info.setApply(allocated = PROVIDER.allocate(info.getSn(), ip, info.getApply()));
				break;
			default:
				allocated = -1;
		}

		SessionData.state(channel, State.SUCCESS);
		return allocated;
	}

	/**
	 * 登录阶段的缓存信息操作及验证
	 */
	private static final class SessionData {

		private static final AttributeKey<TCPInfo> INFO = AttributeKey.newInstance(TCPInfo.class.getSimpleName());

		//当前连接的登录验证码
		private static final AttributeKey<Verifier> VERIFIER = AttributeKey.newInstance(Verifier.class.getSimpleName());
		//连接状态
		private static final AttributeKey<State> STATE = AttributeKey.newInstance(State.class.getSimpleName());

		/**
		 * info
		 */
		private static TCPInfo info(@NonNull Channel channel) {
			return channel.attr(INFO).get();
		}

		private static void info(@NonNull Channel channel, TCPInfo info) {
			channel.attr(INFO).set(info);
		}

		/**
		 * verifier
		 */
		private static Verifier verifier(@NonNull Channel channel) {
			return channel.attr(VERIFIER).get();
		}

		private static void verifier(@NonNull Channel channel, @NonNull Verifier verifier) {
			channel.attr(VERIFIER).set(verifier);
		}

		/**
		 * state
		 */
		private static State state(@NonNull Channel channel) {
			return channel.attr(STATE).get();
		}

		private static void state(@NonNull Channel channel, @NonNull State state) {
			channel.attr(STATE).set(state);
		}

		/**
		 * check apply
		 */
		private static boolean check(@NonNull Device device, Integer apply) {
			switch (device) {
				case APP:
					return true;
				case GATEWAY:
					return apply != null && apply >= Config.TCP_ALLOT_MIN_UDP_PORT;
				default:
					return false;
			}
		}

		/**
		 * check each state data
		 */
		private static boolean check(@NonNull Channel channel, @NonNull State state) {
			if (state(channel) != state) {
				return false;
			}

			TCPInfo info = info(channel);
			if (info == null) {
				return false;//in fact, if state != null then info != null
			}

			switch (state) {
				case CREATE:
				case REQUEST:
					return true;
				case READY://since ready info is only change in apply
				case VERIFY:
				case PASS:
				case WAIT:
				case SUCCESS:
				case CLOSED:
					String sn = info.getSn();
					Device device = info.getDevice();
					Verifier verifier = verifier(channel);
					int apply = info.getApply();
					return sn != null && device != null && verifier != null && check(device, apply);
				default:
					return false;
			}
		}
	}
}
