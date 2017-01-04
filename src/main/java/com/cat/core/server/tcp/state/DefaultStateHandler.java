package com.cat.core.server.tcp.state;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.data.Action;
import com.cat.core.server.data.ErrNo;
import com.cat.core.server.data.Key;
import com.cat.core.server.data.Result;
import com.cat.core.server.tcp.session.SessionHandler;
import io.netty.channel.Channel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "instance")
public final class DefaultStateHandler extends AbstractStateHandler {

	@NonNull
	private final SessionHandler eventHandler;

	@Override
	public void onCreate(@NonNull Channel channel) {
		eventHandler.active(channel);
	}

	@Override
	public void onReady(@NonNull Channel channel, String question) {
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
	public void onVerify(@NonNull Channel channel, boolean result) {
		if (result) {
			super.pass(channel);
			return;
		}

		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.NO.getName());
		json.put(Key.ERROR_NO.getName(), ErrNo.UNKNOWN.getCode());

		channel.writeAndFlush(json);
		super.close(channel);
	}

	@Override
	public void onWait(@NonNull Channel channel) {
		LoginInfo info = ChannelData.info(channel);

		int allocated = 0;
		switch (info.getDevice()) {
			case APP:
				break;
			case GATEWAY:
				allocated = eventHandler.assign(channel);
				break;
			default:
				allocated = -1;
		}

		if (allocated == -1) {
			super.close(channel);
			System.err.println("---------close-------");
			return;
		}

		JSONObject json = new JSONObject();
		json.put(Key.RESULT.getName(), Result.OK.getName());

		if (allocated > 0) {
			json.put(Key.ALLOT.getName(), allocated);
		}

		channel.writeAndFlush(json);

		super.success(channel, allocated);
	}

	@Override
	public void onSuccess(@NonNull Channel channel) {
		eventHandler.register(channel);
	}

	@Override
	public void onClose(@NonNull Channel channel) {
		eventHandler.unRegister(channel);
	}
}
