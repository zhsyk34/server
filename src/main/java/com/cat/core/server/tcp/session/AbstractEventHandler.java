package com.cat.core.server.tcp.session;

import com.cat.core.config.Config;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.State;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.NonNull;

public abstract class AbstractEventHandler implements EventHandler {

	protected abstract void onCreate(@NonNull Channel channel);

	protected abstract void onReady(@NonNull Channel channel, String question);

	protected abstract void onVerify(@NonNull Channel channel, boolean correct);

	protected abstract void onWait(@NonNull Channel channel);

	protected abstract void onSuccess(@NonNull Channel channel);

	protected abstract void onClose(@NonNull Channel channel);

	public final LoginInfo info(@NonNull Channel channel) {
		return Events.info(channel);
	}

	public final boolean finished(@NonNull Channel channel) {
		return Events.state(channel) == State.SUCCESS;
	}

	/**
	 * init session info and save the connect time
	 */
	@Override
	public void create(@NonNull Channel channel) {
		if (Events.premise(channel, State.CREATE)) {
			LoginInfo info = LoginInfo.builder().happen(System.currentTimeMillis()).build();
			Events.info(channel, info);

			Events.state(channel, State.CREATE);

			this.onCreate(channel);
		} else {
			this.close(channel);
		}
	}

	/**
	 * update session info by:sn, device and apply port
	 * and generator the verifier
	 */
	@Override
	public void request(@NonNull Channel channel, @NonNull LoginInfo info) {
		if (Events.premise(channel, State.REQUEST)) {
			//update info
			Events.info(channel).setSn(info.getSn()).setDevice(info.getDevice()).setApply(info.getApply());
			//generator verifier
			Events.verifier(channel, Verifier.generator());

			Events.state(channel, State.REQUEST);

			this.ready(channel);
		} else {
			this.close(channel);
		}
	}

	/**
	 * check the request parameter and response after that int the method : onReady
	 */
	@Override
	public void ready(@NonNull Channel channel) {
		if (Events.premise(channel, State.READY)) {
			String question = Events.verifier(channel).getQuestion();

			Events.state(channel, State.READY);

			this.onReady(channel, question);
		} else {
			this.onReady(channel, null);
			this.close(channel);
		}
	}

	/**
	 * verify the answer(verify code) and response by the result
	 */
	@Override
	public void verify(@NonNull Channel channel, String answer) {
		if (Events.premise(channel, State.VERIFY)) {
			Events.state(channel, State.VERIFY);

			this.onVerify(channel, Events.verifier(channel).getAnswer().equals(answer));
		} else {
			this.close(channel);
		}
	}

	/**
	 * if pass the verify then wait for the follow-up
	 */
	@Override
	public void pass(@NonNull Channel channel) {
		if (Events.premise(channel, State.PASS)) {
			Events.state(channel, State.PASS);

			this.wait(channel);
		} else {
			this.close(channel);
		}
	}

	/**
	 * allocate the port
	 */
	@Override
	public void wait(@NonNull Channel channel) {
		if (Events.premise(channel, State.WAIT)) {
			Events.state(channel, State.WAIT);

			this.onWait(channel);
		} else {
			this.close(channel);
		}
	}

	/**
	 * update the port
	 */
	@Override
	public void success(@NonNull Channel channel, int allocated) {
		if (Events.premise(channel, State.SUCCESS)) {
			Events.info(channel).setApply(allocated);

			Events.state(channel, State.SUCCESS);

			this.onSuccess(channel);
		} else {
			this.close(channel);
		}
	}

	/**
	 * change the state to unRegister
	 * and do other things in the method onClose
	 */
	@Override
	public void close(@NonNull Channel channel) {
		if (Events.premise(channel, State.CLOSED)) {
			Events.state(channel, State.CLOSED);

			this.onClose(channel);
		}
	}

	/**
	 * operate the session info kits
	 */
	private static final class Events {

		private static final AttributeKey<LoginInfo> INFO = AttributeKey.newInstance(LoginInfo.class.getSimpleName());
		private static final AttributeKey<Verifier> VERIFIER = AttributeKey.newInstance(Verifier.class.getSimpleName());
		private static final AttributeKey<State> STATE = AttributeKey.newInstance(State.class.getSimpleName());

		/**
		 * info
		 */
		private static LoginInfo info(@NonNull Channel channel) {
			return channel.attr(INFO).get();
		}

		private static void info(@NonNull Channel channel, LoginInfo info) {
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
		 * check each state data
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
					String sn = info.getSn();
					Device device = info.getDevice();
					Verifier verifier = verifier(channel);
					int apply = info.getApply();
					return sn != null && device != null && verifier != null && check(device, apply);
				default:
					return false;
			}
		}

		/**
		 * premise each state
		 */
		private static boolean premise(@NonNull Channel channel, @NonNull State soon) {
			State current = state(channel);
			return soon.previous() == current && check(channel, current);
		}
	}
}
