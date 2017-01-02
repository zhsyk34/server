package com.cat.core.server.tcp.session;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.ErrNo;
import com.cat.core.server.dict.Key;
import com.cat.core.server.dict.Result;
import io.netty.channel.Channel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(staticName = "instance")
public final class DefaultEventHandler extends AbstractEventHandler {

	/**
	 * register
	 */
	@Override
	protected void onCreate(@NonNull Channel channel) {
		SessionManager.init(channel);
	}

	@Override
	protected void onReady(@NonNull Channel channel, String question) {
		JSONObject json = new JSONObject();

		if (question == null) {
			json.put(Key.RESULT.getName(), Result.NO.getName());
			json.put(Key.ERROR_NO.getName(), ErrNo.PARAMETER.getCode());
		} else {
			json.put(Key.ACTION.getName(), Action.LOGIN_VERIFY.getName());
			json.put(Key.KEY.getName(), question);
		}

		channel.writeAndFlush(json);
	}

	@Override
	protected void onVerify(@NonNull Channel channel, boolean result) {
		if (result) {
			super.pass(channel);
		} else {
			//error verify code
			JSONObject json = new JSONObject();

			json.put(Key.RESULT.getName(), Result.NO.getName());
			json.put(Key.ERROR_NO.getName(), ErrNo.UNKNOWN.getCode());
			channel.writeAndFlush(json);
			super.close(channel);
		}
	}

	@Override
	protected void onWait(@NonNull Channel channel) {
		LoginInfo info = super.info(channel);

		int allocated = 0;
		switch (info.getDevice()) {
			case APP:
				break;
			case GATEWAY:
				//allocate the udp port for gateway client apply
				allocated = SessionManager.allocate(channel);
				break;
			default:
				allocated = -1;
		}

		if (allocated == -1) {
			super.close(channel);
			return;
		}

		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());

		if (allocated > 0) {
			json.put(Key.ALLOT.getName(), allocated);
		}
		super.success(channel, allocated);
		channel.writeAndFlush(json);
	}

	@Override
	protected void onSuccess(@NonNull Channel channel) {
		SessionManager.register(channel);
	}

	/**
	 * un register
	 */
	@Override
	protected void onClose(@NonNull Channel channel) {
		SessionManager.unRegister(channel);
	}
}
