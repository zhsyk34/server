package com.cat.core.server.tcp.port;

import com.cat.core.server.task.TimerTask;

public interface PortHandler {

	/**
	 * get the port2 for sn int the ip
	 */
	int port(String ip, String sn);

	/**
	 * allocate the port2 for ip and sn
	 * if apply is allowable use it direct
	 */
	int allocate(String sn, String ip, int apply);

	/**
	 * when ip change port will allocated again
	 * release history dict
	 */
	TimerTask recycle();
}
