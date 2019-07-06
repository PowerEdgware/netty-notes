package com.netty03;

public class Readme {

	void readme() {
		// LengthFieldBasedFrameDecoder -->head+body pattern
		// LengthFieldPrepender
		// LineBasedFrameDecoder
		// DelimiterBasedFrameDecoder
		// FixedLengthFrameDecoder
		// ObjectEncoder
		// ObjectDecoder

		// TODO 实现 默认的Jdk序列化方式的RPC调用，服务端启动后根据注解@ServiceProvider
		// 查找所有provider并放入容器，启动监听服务。
		// 客户端通过代理封装了连接远程服务的客户端程序，根据请求接口名称，请求的provider，请求参数
		// 实现简单的RPC调用。
	}
}
