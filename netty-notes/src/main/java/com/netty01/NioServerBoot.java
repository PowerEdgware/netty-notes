package com.netty01;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class NioServerBoot {

	public static void main(String[] args) throws IOException {

		final JdkNioServer nioServer = new JdkNioServer(8081);

		new Thread() {
			public void run() {
				try {
					nioServer.start();// 阻塞
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();

		while (!nioServer.isStarted()) {
			Thread.yield();
			LockSupport.parkNanos(nioServer, TimeUnit.MILLISECONDS.toNanos(200));
		}

		Runnable watchRunner = () -> {
			System.out.println(Thread.currentThread().getName() + " started");
			int c = -1;
			while (true) {
				try {
					c = System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (c == 'q') {
					nioServer.stop();
					break;
				}
				System.out.println("Enter q to Stop server");
			}
		};
		Thread watchThread = new Thread(watchRunner, "watcher-thread");
		watchThread.setDaemon(true);
		watchThread.start();

	}
}
