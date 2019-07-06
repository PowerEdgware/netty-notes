package com.netty03.consumer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.netty03.message.RpcRequest;
import com.netty03.message.RpcResponse;

public class RpcProxy {

	final RpcClient rpcClient;

	public RpcProxy(String host, int port) {
		rpcClient = new RpcClient(host, port);
	}

	public void stop() {
		this.rpcClient.stop();
	}

	public <T> T create(Class<T> clazz) {
		if (!clazz.isInterface()) {
			return null;
		}
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new MethodInvoker());
	}

	private class MethodInvoker implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// build req
			RpcRequest request = new RpcRequest();

			request.setClassName(method.getDeclaringClass().getName());
			request.setMethodName(method.getName());
			request.setParames(method.getParameterTypes());
			request.setValues(args);

			RpcResponse response = RpcProxy.this.rpcClient.sendAndGet(request);
			if (response != null) {
				return response.getResponse();
			}
			return null;
		}
	}
}
