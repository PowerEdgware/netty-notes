package com.netty06;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.util.SimpleThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class SimplePressureServer {
	private static final Logger log = LoggerFactory.getLogger(SimplePressureServer.class);

	private int port = 8080;
	private int nThreads = Runtime.getRuntime().availableProcessors() * 4;
	private ChannelFuture future;
	NioEventLoopGroup boss;
	NioEventLoopGroup worker;

	private final SimpleStringHandler childBusnissHandler;

	public SimplePressureServer(int port) {
		this.port = port;
		childBusnissHandler = new SimpleStringHandler();
	}

	public void start() {
		log.warn(" starting server...");
		ServerBootstrap bootstrap = new ServerBootstrap();
		// For test ThreadFactory
		boss = new NioEventLoopGroup(1, SimpleThreadFactory.create("nioBossGroup"));
		worker = new NioEventLoopGroup(nThreads, SimpleThreadFactory.create("nioWorkerGroup"));

		bootstrap.group(boss, worker);

		// bootstrap.handler(handler)
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				// Decoders
				pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(256));
				pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));

				// Encoder
				pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));

				// 消息处理器
				pipeline.addLast(childBusnissHandler);
			}

		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		try {
			// NioEventLoop
			// 绑定端口
			future = bootstrap.bind(this.port).sync();
			SocketAddress bindAddr = future.channel().localAddress();
			log.warn("Server starts on port=" + this.port + " nthreads=" + nThreads + " bindAddr=" + bindAddr);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() throws Exception {
		log.warn("stop server...");
		try {
			future.channel().close().sync();
		} finally {
			worker.shutdownGracefully();
			boss.shutdownGracefully();
		}
		log.warn("server stopped.port=" + port);
	}
}
