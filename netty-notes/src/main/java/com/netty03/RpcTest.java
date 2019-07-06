package com.netty03;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.netty03.api.IBusinessService;
import com.netty03.consumer.RpcProxy;

public class RpcTest {

	public static void main(String[] args) throws UnknownHostException {
		String host = Inet4Address.getByName("localhost").getHostAddress();
		int port = 8080;
		RpcProxy proxy = new RpcProxy(host, port);

		startDaemnThread(proxy);

		IBusinessService iBusinessService = proxy.create(IBusinessService.class);
		Map<String, String> params = new HashMap<String, String>();
		params.put("accountid", "XXX");
		Object result = iBusinessService.doBusiness(params);
		System.out.println(result);
	}

	private static void startDaemnThread(RpcProxy proxy) {
		Thread monitorThread = new Thread(() -> {
			System.out.println(Thread.currentThread().getName() + " started!");
			while (true) {
				int c;
				try {
					c = System.in.read();
					if (c == 's') {
						proxy.stop();
						break;
					}
					System.out.println(Thread.currentThread().getName() + " input 's' to stop");
				} catch (IOException e) {
				}
			}
		}, "monitorThread");

		monitorThread.start();
	}
}
