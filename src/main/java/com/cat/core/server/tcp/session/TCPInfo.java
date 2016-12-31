package com.cat.core.server.tcp.session;

import com.alibaba.fastjson.JSONObject;
import com.cat.core.server.dict.Device;
import com.cat.core.server.dict.Key;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 网关TCP会话信息
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Accessors(chain = true)
public final class TCPInfo {
	private String sn;
	private Device device;
	private int apply;
	private long happen;

//	private final String ip;
//	private final int port;

//	static TCPInfo init(InetSocketAddress address) {
//		return new TCPInfo(address.getAddress().getHostAddress(), address.getPort(), System.currentTimeMillis());
//	}

	//TODO
	public static TCPInfo from(JSONObject json) {
		Device device = Device.from(json.getIntValue(Key.TYPE.getName()));
		String sn = json.getString(Key.SN.getName());
		Integer port = json.getInteger(Key.ALLOT.getName());
		return new TCPInfo();
	}
}