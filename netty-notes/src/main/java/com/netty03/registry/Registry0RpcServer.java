package com.netty03.registry;

import java.net.SocketAddress;

import com.netty03.codec.BusinessHandler;
import com.util.SimpleThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class Registry0RpcServer {
	private int port = 8080;
	private int nThreads = Runtime.getRuntime().availableProcessors() * 4;
	private ChannelFuture future;
	NioEventLoopGroup boss;
	NioEventLoopGroup worker;

	private final BusinessHandler childBusnissHandler;

	public Registry0RpcServer() {
		childBusnissHandler = new BusinessHandler();
	}

	public void start() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		//For test ThreadFactory
		boss = new NioEventLoopGroup(1, SimpleThreadFactory.create("nioBossGroup"));
		worker = new NioEventLoopGroup(nThreads, SimpleThreadFactory.create("nioWorkerGroup"));

		bootstrap.group(boss, worker);

		// bootstrap.handler(handler)
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				// 为新连接设置编码/解码器||消息处理器等
				ChannelPipeline p = ch.pipeline();
				// 解析java默认序列化方式的对象
				p.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
				p.addLast("objectDecoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
				// 反序列化：编码
				p.addLast(new LengthFieldPrepender(4));
				// object 反序列化：编码
				p.addLast("objectEncoder", new ObjectEncoder());

				// 消息处理器
				p.addLast(childBusnissHandler);

			}

		}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		try {
			// NioEventLoop
			// 绑定端口
			future = bootstrap.bind(this.port).sync();
			SocketAddress bindAddr = future.channel().localAddress();
			System.out
					.println("Server starts on port=" + this.port + " nthreads=" + nThreads + " bindAddr=" + bindAddr);
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
		Registry0RpcServer rpcServer = new Registry0RpcServer();
		Thread start = new Thread(() -> {
			rpcServer.start();
		});
		start.start();

		try {
			while (true) {
				System.out.println("Enter q to stop server.");
				int c = System.in.read();
				if (c == 'q') {
					rpcServer.stop();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
