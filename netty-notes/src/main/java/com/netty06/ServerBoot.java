package com.netty06;

public class ServerBoot {
	private final SimplePressureServer server;

	public ServerBoot(SimplePressureServer server) {
		this.server = server;
	}

	public static void main(String[] args) {
		int port = 7720;
		int nthreads=Runtime.getRuntime().availableProcessors()*2;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
			nthreads = Integer.parseInt(args[1]);
		}
		SimplePressureServer rpcServer = new SimplePressureServer(port,nthreads);
		ServerBoot boot = new ServerBoot(rpcServer);

		Thread start = new Thread(() -> {
			rpcServer.start();
		});
		start.start();

		boot.addHook();

	}

	private void addHook() {
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private void removeHook() {
		if (this.shutdownHook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			} catch (IllegalStateException ex) {
			}
		}
	}

	private Thread shutdownHook = new Thread() {
		public void run() {
			try {
				destory();
				removeHook();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	};

	private void destory() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	};

}
