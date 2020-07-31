package com.netty06.cli;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

public class NettyClient {
	static final Logger log = LoggerFactory.getLogger(NettyClient.class);

	// channel 连接链路池子
	private List<Channel> clientPool = new CopyOnWriteArrayList<>();

	private int nThreads = Runtime.getRuntime().availableProcessors() * 2;
	private int connectionTimeout = 50;// sec
	private int readTimeout = 15;// sec
	private final String host;
	private final int port;

	private Bootstrap bootstrap;
	private NioEventLoopGroup group;

	private AtomicBoolean started = new AtomicBoolean(false);

	public NettyClient(String host, int port, int nThreads) {
		this.port = port;
		this.host = host;
		if (nThreads > 0) {
			this.nThreads = nThreads;
		}
	}

	public void start() throws InterruptedException {
		log.info(" starts connect to=" + host + " port=" + port);
		started.set(true);

		bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup(this.nThreads);
		bootstrap.group(group);

		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.channel(NioSocketChannel.class).handler(new ClientChannelInitializer());

		// 连接
		buildConnPool();

		log.info(" starts over for host=" + host + " port=" + port + " clientpoolsize=" + clientPool.size());
	}

	public boolean isStarted() {
		return started.get();
	}

	AtomicInteger sucNum=new AtomicInteger();
	private void buildConnPool() {
		for (int i = 0; i < nThreads; i++) {
			ChannelFuture future = bootstrap.connect(host, port);
			// TODO 添加监听器，如果连接失败，则需要重新连接
//			future.addListener(listener)
			// 等待连接成功
			boolean suc = future.awaitUninterruptibly(connectionTimeout, TimeUnit.SECONDS);
			if (suc && future.isSuccess()) {
				Channel channel = future.channel();
				log.info("channel connected:" + channel.localAddress() + ", remote=" + channel.remoteAddress()
						+ " index=" + i);
				clientPool.add(future.channel());
				sucNum.incrementAndGet();
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(130));
			} else {
				log.warn("connect failed: " + this.host + ":" + this.port + " nWorkers=" + nThreads + " index=" + i,future.cause());
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
			}
			// 是否停止了
			if (!started.get()) {
				log.info(" started set to false break connection...");
				break;
			}
		}
	}
	
	public int getConnSucNum() {
		return sucNum.get();
	}

	public void stop() {
		if (!started.get()) {
			log.warn("client is not start...");
			return;
		}
		started.set(false);
		try {
			log.warn("stopping client..");
			for (Channel channel : clientPool) {
				try {
					channel.close().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			log.warn("client is stopped...");
		} finally {
			group.shutdownGracefully();
		}
	}

	// simple send msg
	public void senMsg(String msg) {
		// get client
		int index = new Random(System.currentTimeMillis()).nextInt(clientPool.size());

		Channel channel = clientPool.get(index);
		if (channel.isWritable()) {
			log.info("channel=" + channel + " Thread=" + Thread.currentThread().getName() + " sendmsg=" + msg);
			channel.writeAndFlush(msg);
		} else {
			channel.close();
			log.error("channel=" + channel + " not writable.");
		}
	}

	// 连接初始化
	private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {

			ChannelPipeline pipeline = ch.pipeline();
			// Decoders
			pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(256));
			pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));

			// Encoder
			pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));

			// 客户端消息处理器
			pipeline.addLast("handler", new ClientHandler());
		}

	}

	/*
	 * 消息处理器
	 */
	private class ClientHandler extends SimpleChannelInboundHandler<String> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, String resp) throws Exception {
			log.info("resp=" + resp + " from server:" + ctx.channel().remoteAddress());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			// TODO 一些回调处理 比如重连
			log.warn("ch=" + ctx.channel().remoteAddress().toString() + " error");
			try {
				ctx.close();
				cause.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
