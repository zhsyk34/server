package com.cat.core.server.tcp;

import com.cat.core.kit.ThreadKit;
import com.cat.core.server.Controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerTest {

	private static final Controller controller = Controller.instance();

	public static void main(String[] args) {

		ExecutorService service = Executors.newCachedThreadPool();
//		service.submit(UDPServer::start);
//		while (UDPServer.getChannel() == null) {
//			System.out.println("udp server start...");
//			ThreadKit.await(500);
//		}

		service.submit(TCPServer::start);
		while (!TCPServer.isStarted()) {
			System.out.println("tcp server start...");
			ThreadKit.await(500);
		}

		service.shutdown();

//		controller.task();
	}
}