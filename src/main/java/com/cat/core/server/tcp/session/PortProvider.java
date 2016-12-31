package com.cat.core.server.tcp.session;

@FunctionalInterface
public interface PortProvider {
	int allocate(String sn, String ip, int apply);
}
