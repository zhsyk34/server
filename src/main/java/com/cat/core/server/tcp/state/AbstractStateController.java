package com.cat.core.server.tcp.state;

import com.cat.core.server.dict.State;
import io.netty.channel.Channel;
import lombok.NonNull;

/**
 * unrealized method as follow:
 * onCreate,onReady,onVerify,onWait,onSuccess,onClose
 */
abstract class AbstractStateController implements StateController {

	/**
	 * active session info and save the connect time
	 */
	@Override
	public void create(@NonNull Channel channel) {
		if (ChannelData.premise(channel, State.CREATE)) {
			LoginInfo info = LoginInfo.builder().happen(System.currentTimeMillis()).build();
			ChannelData.info(channel, info);

			ChannelData.state(channel, State.CREATE);

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
		if (ChannelData.premise(channel, State.REQUEST)) {
			//update info
			ChannelData.info(channel).setSn(info.getSn()).setDevice(info.getDevice()).setApply(info.getApply());
			//generator verifier
			ChannelData.verifier(channel, Verifier.generator());

			ChannelData.state(channel, State.REQUEST);

			this.onRequest(channel);
		} else {
			this.close(channel);
		}
	}

	@Override
	public void onRequest(@NonNull Channel channel) {
		this.ready(channel);
	}

	/**
	 * check the request parameter and response after that in the method : onReady
	 */
	@Override
	public void ready(@NonNull Channel channel) {
		if (ChannelData.premise(channel, State.READY)) {
			String question = ChannelData.verifier(channel).getQuestion();

			ChannelData.state(channel, State.READY);

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
		if (ChannelData.premise(channel, State.VERIFY)) {
			ChannelData.state(channel, State.VERIFY);

			this.onVerify(channel, ChannelData.verifier(channel).getAnswer().equals(answer));
		} else {
			this.close(channel);
		}
	}

	/**
	 * if pass the verify then wait for the follow-up
	 */
	@Override
	public void pass(@NonNull Channel channel) {
		if (ChannelData.premise(channel, State.PASS)) {
			ChannelData.state(channel, State.PASS);

			this.onPass(channel);
		} else {
			this.close(channel);
		}
	}

	@Override
	public void onPass(@NonNull Channel channel) {
		this.wait(channel);
	}

	/**
	 * allocate the port
	 */
	@Override
	public void wait(@NonNull Channel channel) {
		if (ChannelData.premise(channel, State.WAIT)) {
			ChannelData.state(channel, State.WAIT);

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
		if (ChannelData.premise(channel, State.SUCCESS)) {
			ChannelData.info(channel).setApply(allocated);

			ChannelData.state(channel, State.SUCCESS);

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
		if (ChannelData.premise(channel, State.CLOSED)) {
			ChannelData.state(channel, State.CLOSED);

			channel.close();
			this.onClose(channel);
		}
	}
}
