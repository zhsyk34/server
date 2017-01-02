package com.cat.core.server.udp.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetSocketAddress;

/**
 * 网关UDP心跳信息
 */
@RequiredArgsConstructor(staticName = "of")
@Getter
@Setter
public final class UDPInfo {
	private final String sn;

	private final String ip;
	private final int port;

	private final String version;
	private final long happen;

	public static UDPInfo from(String sn, InetSocketAddress address, String version) {
		String ip = address.getAddress().getHostAddress();
		int port = address.getPort();
		return UDPInfo.of(sn, ip, port, version, System.currentTimeMillis());
	}
}
