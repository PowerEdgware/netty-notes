package com.netty06;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class SimpleStringHandler extends SimpleChannelInboundHandler<String> {

	static final Logger log = LoggerFactory.getLogger(SimpleStringHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		// 直接写回
		log.info("Recved msg=" + msg + " from " + ctx.channel().remoteAddress());
		String retMsg = "Hello,client." + msg + " dateTime=" + LocalDateTime.now() + "\r\n";
		ctx.writeAndFlush(retMsg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		log.error("exception caught! channel=" + channel.localAddress() + " remote=" + channel.remoteAddress(), cause);
	}

}
