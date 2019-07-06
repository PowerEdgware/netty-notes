package com.netty03.consumer;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.netty03.message.MessageRequest;
import com.netty03.message.MessageResponse;
import com.netty03.message.RpcRequest;
import com.netty03.message.RpcResponse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RpcClient {
	// channel 连接链路池子
	private List<Channel> clientPool = new CopyOnWriteArrayList<>();

	// 消息同步器
	private ConcurrentMap<String, CompletableFuture<MessageResponse>> futureMsgMap = new ConcurrentHashMap<>();

	private int nThreads = Runtime.getRuntime().availableProcessors() * 2;
	private int connectionTimeout = 10;// sec
	private int readTimeout = 15;// sec
	private final String host;
	private final int port;

	private Bootstrap bootstrap;
	private NioEventLoopGroup group;

	public RpcClient(String host, int port) {
		this.port = port;
		this.host = host;
		try {
			this.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void start() throws InterruptedException {
		bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup(this.nThreads);
		bootstrap.group(group);

		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.channel(NioSocketChannel.class).handler(new ClientChannelInitializer());

		// 连接
		buildConnPool();
	}

	private void buildConnPool() {
		for (int i = 0; i < nThreads; i++) {
			ChannelFuture future = bootstrap.connect(host, port);
			// TODO 添加监听器，如果连接失败，则需要重新连接
//			future.addListener(listener)
			// 等待连接成功
			boolean suc = future.awaitUninterruptibly(connectionTimeout, TimeUnit.SECONDS);
			if (suc) {
				Channel channel = future.channel();
				System.out
						.println("channel connected:" + channel.localAddress() + ", remote=" + channel.remoteAddress());
				clientPool.add(future.channel());
			} else {
				System.out.println("connect failed: " + this.host + ":" + this.port);
				break;
			}
		}
	}

	public void stop() {
		try {
			for (Channel channel : clientPool) {
				try {
					channel.close().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			group.shutdownGracefully();
		}
	}

	public RpcResponse sendAndGet(RpcRequest request) {
		// get client
		int index = new Random(System.currentTimeMillis()).nextInt(clientPool.size());

		String msgId = UUID.randomUUID().toString();
		MessageRequest req = new MessageRequest();
		req.setMsgId(msgId);
		req.setRequest(request);

		Channel channel = clientPool.get(index);
		if (channel.isWritable()) {
			channel.writeAndFlush(req);
		} else {
			return null;
		}
		CompletableFuture<MessageResponse> future = new CompletableFuture<>();
		futureMsgMap.put(msgId, future);
		// 阻塞等待
		MessageResponse resp = null;
		try {
			resp = future.get(readTimeout, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			futureMsgMap.remove(msgId);
		}
		RpcResponse response = new RpcResponse();
		if (resp == null) {
			response.setCode(0);
		}
		response.setResponse(resp.getResponse());

		return response;
	}

	// 连接初始化
	private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			// 设置处理器，编解码器
			pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
			// 自定义协议编码器
			pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
			// 对象参数类型编码器
			pipeline.addLast("encoder", new ObjectEncoder());
			// 对象参数类型解码器|jdk默认的对象序列化和反序列化方式
			pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));

			// 客户端消息处理器
			pipeline.addLast("handler", new ClientHandler());
		}

	}

	/*
	 * 消息处理器
	 */
	private class ClientHandler extends SimpleChannelInboundHandler<MessageResponse> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, MessageResponse resp) throws Exception {
			System.out.println("resp msg=" + resp);
			RpcClient.this.futureMsgMap.get(resp.getMsgId()).complete(resp);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO 一些回调处理 比如重连
			ctx.close();
			cause.printStackTrace();
		}

	}
}
