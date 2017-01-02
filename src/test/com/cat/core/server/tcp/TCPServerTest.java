package com.cat.core.server.tcp;

import com.cat.core.kit.ThreadKit;
import com.cat.core.server.tcp.session.SessionManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TCPServerTest {

	public static void main(String[] args) {
		ScheduledExecutorService service = Executors.newScheduledThreadPool(4);

		service.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			System.out.println("tcp server start...");
			ThreadKit.await(1000);
		}

		service.scheduleAtFixedRate(SessionManager::monitor, 1, 5, TimeUnit.SECONDS);
	}
}