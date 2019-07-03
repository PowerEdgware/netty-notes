package com.netty02;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

//ChannelInboundHandlerAdapter 
//SimpleChannelInboundHandler
public class LocalHttpRequestHandler extends ChannelInboundHandlerAdapter {

	// 读取数据
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// 解析消息
		if(msg instanceof HttpRequest) {
			HttpRequest req=(HttpRequest) msg;
			String uri=req.uri();
			
			
		}
	}
}
