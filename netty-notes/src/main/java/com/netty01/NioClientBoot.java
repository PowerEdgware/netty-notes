package com.netty01;

import java.io.IOException;
import java.util.Scanner;

public class NioClientBoot {

	public static void main(String[] args) throws IOException {
		final JdkNioClient client = new JdkNioClient(8081);

		Thread writeThread = new Thread(() -> {
			while (true) {
				// 在主线程中 从键盘读取数据输入到服务器端
				Scanner scan = new Scanner(System.in);
				while (scan.hasNextLine()) {
					String line = scan.nextLine();
					if ("".equals(line))
						continue; // 不允许发空消息
					else if ("q".equals(line)) {
						break;
					}
					try {
						client.writeData(line);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
				scan.close();
			}
		});
		writeThread.start();
	}
}
