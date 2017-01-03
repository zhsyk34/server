package com.cat.core.server.tcp;

import com.cat.core.kit.ThreadKit;
import com.cat.core.server.tcp.session.DefaultSessionHandler;

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

//		service.submit(UDPServer::start);
//		while (UDPServer.getChannel() == null) {
//			System.out.println("udp server start...");
//			ThreadKit.await(1000);
//		}

//		service.submit(() -> {
//			while (true) {
//				DefaultMessageHandler.process();
//			}
//		});
//
//		service.scheduleAtFixedRate(DefaultMessageHandler::total, 1, 6, TimeUnit.SECONDS);
//
		service.scheduleAtFixedRate(() -> DefaultSessionHandler.instance().monitor(null), 1, 5, TimeUnit.SECONDS);
	}
}