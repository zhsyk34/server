package com.cat.core.server.udp.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.net.InetSocketAddress;

/**
 * udp heart beat info
 */
@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
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
