package com.cat.core.server.tcp.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Action;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.Key;
import com.cat.core.server.tcp.state.ChannelData;
import com.cat.core.server.tcp.state.LoginInfo;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

final class GatewayInfo {

	static String login(@NonNull Channel channel) {
		LoginInfo info = ChannelData.info(channel);

		if (info.getDevice() != Device.GATEWAY) {
			return null;
		}

		String ip = ChannelData.ip(channel);
		int port = ChannelData.port(channel);
		GatewayLoginInfo data = GatewayLoginInfo.of(ip, port, info.getSn(), info.getApply(), info.getHappen());

		if (data == null) {
			return null;
		}

		JSONObject json = (JSONObject) JSON.toJSON(data);
		json.put(Key.ACTION.getName(), Action.TCP_LOGIN_PUSH.getName());

		return json.toString();
	}

	static String logout(@NonNull Channel channel) {
		LoginInfo info = ChannelData.info(channel);

		if (info.getDevice() != Device.GATEWAY) {
			return null;
		}

		GatewayLogoutInfo data = GatewayLogoutInfo.of(info.getSn(), System.currentTimeMillis());

		if (data == null) {
			return null;
		}

		JSONObject json = (JSONObject) JSON.toJSON(data);
		json.put(Key.ACTION.getName(), Action.TCP_LOGOUT_PUSH.getName());

		return json.toString();
	}

	@AllArgsConstructor(staticName = "of")
	@Getter
	private static final class GatewayLoginInfo {
		private final String ip;
		private final int port;
		private final String sn;
		private final int apply;
		private final long happen;
	}

	@AllArgsConstructor(staticName = "of")
	@Getter
	private static final class GatewayLogoutInfo {
		private final String sn;
		private final long happen;
	}
}



