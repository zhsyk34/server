package com.cat.core.server.tcp.session;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.Key;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * tcp session info
 */
@Getter
@Setter
@Builder
@Accessors(chain = true)
@ToString
public final class LoginInfo {
	private String sn;
	private Device device;
	private int apply;
	private long happen;

	public static LoginInfo from(JSONObject json) {
		String sn = json.getString(Key.SN.getName());
		Device device = Device.from(json.getIntValue(Key.TYPE.getName()));
		Integer apply = json.getInteger(Key.ALLOT.getName());
		return LoginInfo.builder().sn(sn).device(device).apply(apply == null ? 0 : apply).build();
	}
}