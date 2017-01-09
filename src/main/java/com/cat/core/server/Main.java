package com.cat.core.server;

import com.cat.core.config.Config;
import com.cat.core.kit.ThreadKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.tcp.TCPServer;
import com.cat.core.server.udp.UDPServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) {

		ExecutorService service = Executors.newCachedThreadPool();

		//udp server
		service.submit(UDPServer::start);
		while (UDPServer.getChannel() == null) {
			Log.logger(Factory.UDP_EVENT, UDPServer.class.getSimpleName() + " is starting...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME);
		}

		//tcp server
		service.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			Log.logger(Factory.TCP_EVENT, TCPServer.class.getSimpleName() + " is starting...");
			ThreadKit.await(Config.SERVER_START_MONITOR_TIME);
		}

		service.shutdown();

		Controller.instance().task();
	}
}
