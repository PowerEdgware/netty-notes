package com.netty02;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class NettyTomcatServer {
	private int port = 8080;
	private int nThreads = Runtime.getRuntime().availableProcessors() * 4;
	private ChannelFuture future;
	NioEventLoopGroup boss;
	NioEventLoopGroup worker;

	public void start() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		boss = new NioEventLoopGroup(1);
		worker = new NioEventLoopGroup(nThreads);

		bootstrap.group(boss, worker);

		// bootstrap.handler(handler)
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				// 为新连接设置编码/解码器||消息处理器等
				ChannelPipeline p = ch.pipeline();
				p.addLast("decoder", new HttpRequestDecoder());
				p.addLast("encoder", new HttpResponseEncoder());
				p.addLast("aggregator", new HttpObjectAggregator(1048576));
				// 消息处理器
				p.addLast("handler", new LocalHttpRequestHandler());

			}

		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		try {
			// NioEventLoop
			// 绑定端口
			future = bootstrap.bind(this.port).sync();
			System.out.println("Server starts on port=" + this.port + " nthreads=" + nThreads);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() throws Exception {
		try {
			future.channel().close().sync();
		} finally {
			worker.shutdownGracefully();
			boss.shutdownGracefully();
		}
		System.out.println("server stopped");
	}

	public static void main(String[] args) {
		NettyTomcatServer tomcatServer = new NettyTomcatServer();
		Thread start = new Thread(() -> {
			tomcatServer.start();
		});
		start.start();

		try {
			while (true) {
				System.out.println("Enter q to stop server.");
				int c = System.in.read();
				if (c == 'q') {
					tomcatServer.stop();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
