package com.netty01;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JdkNioServer {

	private final Selector selector;// 连接管理器
	private final ServerSocketChannel serverSocketChannel;
	private final int port;
	private AtomicBoolean stopped = new AtomicBoolean(false);

	ExecutorService executor = Executors.newCachedThreadPool();

	private volatile Thread runnerThread;

	public JdkNioServer(int port) throws IOException {
		this.port = port;
		this.selector = Selector.open();
		serverSocketChannel = SelectorProvider.provider().openServerSocketChannel();

		serverSocketChannel.configureBlocking(false);
//		InetAddress addr = InetAddress.getByName("127.0.0.1");
		InetSocketAddress socketAddress = new InetSocketAddress(this.port);
		serverSocketChannel.socket().bind(socketAddress, 128);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// 注册selector，交给其管理
		System.out.println("Nio Server bind on port=" + port);
	}

	public boolean isStarted() {
		return runnerThread != null;
	}

	public void start() throws IOException {
		try {
			runnerThread = Thread.currentThread();
			while (!stopped.get()) {
				try {
					int eventNum = selector.select(TimeUnit.SECONDS.toMillis(1));
					if (stopped.get()) {
						break;
					}
					if (eventNum <= 0) {
						continue;
					}
				} catch (Exception e) {
					if (stopped.get()) {
						break;
					}
				}
				// 获取事件
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> itr = keys.iterator();
				while (itr.hasNext()) {
					SelectionKey eventkey = itr.next();
					itr.remove();
					if (eventkey.isAcceptable()) {// 可连接事件
						// 获取socket,TODO accept()是非阻塞的
						SocketChannel socketChannel = JdkNioServer.this.channel().accept();
						System.out.println("[new client connected] client:" + socketChannel.getRemoteAddress());
						socketChannel.configureBlocking(false);
						socketChannel.register(JdkNioServer.this.selector(), SelectionKey.OP_READ);// 交给selector管理感兴趣的事件

						socketChannel.write(
								Charset.defaultCharset().encode("welcome client." + socketChannel.getRemoteAddress()));
					} else {
						executor.submit(new EventRunner(eventkey));
						// new EventRunner(eventkey).run();
					}
				}

			}
		} finally {
			this.channel().close();
			this.selector().close();
		}
	}

	private ServerSocketChannel channel() {
		return this.serverSocketChannel;
	}

	private Selector selector() {
		return this.selector;
	}

	public boolean stop() {
		System.out.println("Recevd stop singal from:" + Thread.currentThread().getName());
		stopped.set(true);
		if (runnerThread != null) {
			runnerThread.interrupt();
		}
		return true;
	}

	class EventRunner implements Runnable {
		private SelectionKey eventKey;

		public EventRunner(SelectionKey eventKey) {
			this.eventKey = eventKey;
		}

		@Override
		public void run() {
			try {
				processEvent();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		void processEvent() throws IOException {
			if (eventKey.isReadable()) {
				// do read
				ByteBuffer buf = ByteBuffer.allocateDirect(512);

				SocketChannel socketChannel = (SocketChannel) eventKey.channel();
				StringBuffer content = new StringBuffer();
				Charset charset = Charset.defaultCharset();
				// 写入Buf
				while (socketChannel.read(buf) > 0) {
					buf.flip();//// 转换模式，由写变成读取
					content.append(charset.decode(buf));
				}
				if (content.length() == 0) {
					return;
				}

//				if (buf.hasArray()) {
//					bytes = buf.array();
//				} else {
//					buf.get(bytes);
//				}
//				byte[] bytes = new byte[buf.limit()];
				System.out.println(buf);
//				buf.get(bytes);

				// StringBuffer sub = new StringBuffer(Charset.defaultCharset().decode(buf));
				System.out.println("Server recved data=" + content.toString() + " len=" + content.length()
						+ " from chn=" + socketChannel.getRemoteAddress());

				buf.clear();
				buf = null;
				eventKey.interestOps(SelectionKey.OP_WRITE);

				String data = "server resp:hello client received data:[" + content + "]at:" + LocalDateTime.now();
				// 写数据
				socketChannel.write(ByteBuffer.wrap(data.getBytes()));
//				socketChannel.socket().getOutputStream().flush();

			} else if (eventKey.isWritable()) {
//				String data = "server resp:hello,client at:" + LocalDateTime.now();
//				// 写数据
//
//				SocketChannel socketChannel = (SocketChannel) eventKey.channel();
//				socketChannel.write(ByteBuffer.wrap(data.getBytes()));
////				socketChannel.close();
//				eventKey.interestOps(SelectionKey.OP_READ);
			} else if (!eventKey.isValid()) {
				eventKey.channel().close();
			}

		}
	}
}
