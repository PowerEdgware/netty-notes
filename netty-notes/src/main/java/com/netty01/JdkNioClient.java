package com.netty01;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class JdkNioClient {

	private Selector connSelector;
	private SocketChannel socketChannel;
	private int port;

	public JdkNioClient(int connectionPort) throws IOException {
		this.port = connectionPort;
		connSelector = Selector.open();
		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), this.port);
		socketChannel = SocketChannel.open(remote);// connect
		socketChannel.configureBlocking(false);
		socketChannel.register(connSelector, SelectionKey.OP_CONNECT);
		socketChannel.register(connSelector, SelectionKey.OP_READ);
		socketChannel.finishConnect();

		System.out.println("Client Connected." + socketChannel.getLocalAddress());
		startRead();
	}

	public void writeData(String data) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(data.length());
		socketChannel.write(buffer);
		socketChannel.socket().getOutputStream().flush();
	}

	private void startRead() {
		new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						connSelector.select();
					} catch (Exception e) {
						e.printStackTrace();
					}

					Iterator<?> selectedKeys = connSelector.selectedKeys().iterator();
					while (selectedKeys.hasNext()) {
						SelectionKey key = (SelectionKey) selectedKeys.next();
						selectedKeys.remove();
						if (!key.isValid()) {
							continue;
						}
						dispatch(key);
					}
				}

			}
		}.start();
	}

	private void dispatch(SelectionKey key) {
		try {
			if (key.isConnectable()) {
				System.out.println("[event]connect.");
				SocketChannel socketChannel = (SocketChannel) key.channel();
				// 判断连接是否建立成功，不成功会抛异常
				socketChannel.finishConnect();
				// 设置Key的interest set为OP_WRITE事件
				// key.interestOps(SelectionKey.OP_READ);
				socketChannel.register(connSelector, SelectionKey.OP_READ);

			} else if (key.isReadable()) {
				System.out.println("[event]read");
				ByteBuffer buf = ByteBuffer.allocate(1024);
				int bytesLen = socketChannel.read(buf);
				if (bytesLen <= 0) {
					System.out.println("no data");
					return;
				}
				SocketAddress remoteAddr = socketChannel.getRemoteAddress();

				buf.flip();// 读取数据
				Charset chaset = Charset.defaultCharset();
				CharBuffer charBuffer = chaset.decode(buf);
				StringBuilder content = new StringBuilder();
				content.append(charBuffer);
				System.out.println("[Client receive from server:" + remoteAddr + "] -> content: " + content);
			}
		} catch (IOException e) {
			e.printStackTrace();
			key.channel();
			try {
				if (key.channel() != null) {
					key.channel().close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
