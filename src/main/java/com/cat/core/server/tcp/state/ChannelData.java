package com.cat.core.server.tcp.state;

import com.cat.core.config.Config;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.State;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.NonNull;

import java.net.InetSocketAddress;

/**
 * operate the session info kits
 */
public final class ChannelData {
	private static final AttributeKey<LoginInfo> LOGIN_INFO = AttributeKey.newInstance(LoginInfo.class.getSimpleName());
	private static final AttributeKey<Verifier> VERIFIER = AttributeKey.newInstance(Verifier.class.getSimpleName());
	private static final AttributeKey<State> STATE = AttributeKey.newInstance(State.class.getSimpleName());

	/*---------------------------------------base data by channel self---------------------------------------*/

	public static String id(@NonNull Channel channel) {
		return channel.id().asShortText();
	}

	public static String ip(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	public static int port(@NonNull Channel channel) {
		return ((InetSocketAddress) channel.remoteAddress()).getPort();
	}

	/*---------------------------------------cache data for login---------------------------------------*/

	/**
	 * info
	 */
	public static LoginInfo info(@NonNull Channel channel) {
		return channel.attr(LOGIN_INFO).get();
	}

	static void info(@NonNull Channel channel, LoginInfo info) {
		channel.attr(LOGIN_INFO).set(info);
	}

	/**
	 * verifier
	 */
	static Verifier verifier(@NonNull Channel channel) {
		return channel.attr(VERIFIER).get();
	}

	static void verifier(@NonNull Channel channel, @NonNull Verifier verifier) {
		channel.attr(VERIFIER).set(verifier);
	}

	/**
	 * state
	 */
	public static State state(@NonNull Channel channel) {
		return channel.attr(STATE).get();
	}

	static void state(@NonNull Channel channel, @NonNull State state) {
		channel.attr(STATE).set(state);
	}


	/*---------------------------------------check kit---------------------------------------*/

	/**
	 * check apply port
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
	 * check each state dict
	 */
	private static boolean check(@NonNull Channel channel, State state) {
		if (state == null || state == State.CLOSED) {
			return true;
		}

		if (state(channel) != state) {
			return false;
		}

		LoginInfo info = info(channel);
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
				Device device = info.getDevice();
				return info.getSn() != null && device != null && verifier(channel) != null && check(device, info.getApply());
			default:
				return false;
		}
	}

	/**
	 * premise each state
	 */
	static boolean premise(@NonNull Channel channel, @NonNull State soon) {
		State current = state(channel);
		return soon.previous() == current && check(channel, current);
	}
}