package com.netty06.cli;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientBoot {
	static final Logger log = LoggerFactory.getLogger(ClientBoot.class);

	static final private class Blocker {
	};

	static final Blocker blocker = new Blocker();

	static final int SendnThreads = 20;
	static ExecutorService es = Executors.newFixedThreadPool(SendnThreads);
	static volatile boolean stopEs = false;

	public static void main(String[] args) {
		log.info("Main params=" + Arrays.toString(args));
//		String remote = args[0];
//		int port = Integer.parseInt(args[1]);
//		int nThreads = Integer.parseInt(args[2]);
		
		String remote ="192.168.43.241";
		int port =9091;
		int nThreads = 100;
		
		final NettyClient nettyClient = new NettyClient(remote, port, nThreads);
		// start thread
		new Thread() {
			public void run() {
				try {
					nettyClient.start();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();

		startMonitorThread(nettyClient);

		startSendThread(nettyClient);

	}

	private static void startSendThread(NettyClient nettyClient) {

		for (int i = 0; i < SendnThreads; i++) {
			es.submit(() -> {
				while (true) {
					if (stopEs) {
						log.info(Thread.currentThread().getName() + " stoped send...At=" + LocalDateTime.now());
						break;
					}
					if (!nettyClient.isStarted()) {
						LockSupport.parkNanos(blocker, TimeUnit.SECONDS.toNanos(1));
						continue;
					}
					String msg = randMsg(Thread.currentThread().getName());
					try {
						nettyClient.senMsg(msg);
					} catch (Exception e) {
					}
					System.out.println("Conn SUC NUM="+nettyClient.getConnSucNum());
					Random rnd = new Random();
					long duration = rnd.nextInt(5) + 1;
					LockSupport.parkNanos(blocker, TimeUnit.SECONDS.toNanos(duration));
				}
			});

		}

	}

	static String fullabc = "abcdefghijklmnopqrstuvwxyz01234567789";

	private static String randMsg(String threadname) {
		StringBuffer buf = new StringBuffer();
		Random sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if (sr == null) {
			sr = new Random(System.currentTimeMillis());
		}
		int rndLen = threadname.length() + sr.nextInt(16);
		for (int i = 0; i < rndLen; i++) {
			buf.append(fullabc.charAt(sr.nextInt(fullabc.length())));
		}
		buf.append("\r\n");
		return buf.toString();
	}

	private static void startMonitorThread(final NettyClient nettyClient) {
		Thread minitor = new Thread() {
			public void run() {
				while (true) {
					try {
						int c = System.in.read();
						if (c == 'q') {
							stopEs = true;
							es.shutdown();
							nettyClient.stop();
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
		};
		minitor.start();
	}

}
