package com.netty02;

import java.io.FileInputStream;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

//ChannelInboundHandlerAdapter 
//SimpleChannelInboundHandler
/**
 * 
 * 
 * HTTP 消息处理类，得熟悉HTTP协议且熟悉Netty对它的封装，这里只是简单实现txt文件的写出
 * 可以直接使用：SimpleChannelInboundHandler
 */
public class LocalHttpRequestHandler extends ChannelInboundHandlerAdapter {

	// 读取数据
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		// 解析消息
		if (msg instanceof HttpRequest) {

			HttpRequest request = (HttpRequest) msg;
			System.out.println("request=" + request);
			FullHttpResponse response = null;
			String resp = "";
			try {
				String uri = request.uri();
				FileInputStream fis = new FileInputStream("files/" + uri + ".txt");
				byte[] datas = fis.readAllBytes();
				fis.close();

				resp = new String(datas);
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
						Unpooled.wrappedBuffer(resp.getBytes()));
				setHeaders(response, request);
			} catch (Exception e) {// 处理出错，返回错误信息
				resp = "<html><body>Server Error reason:" + e + "</body></html>";
				response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
						Unpooled.wrappedBuffer(resp.getBytes()));
				setHeaders(response, request);

			}
			if (response != null)
				ctx.writeAndFlush(response);
		}
		if (msg instanceof HttpContent) {
			HttpContent content = (HttpContent) msg;
			ByteBuf buf = content.content();
			System.out.println("context-->" + buf.toString(CharsetUtil.UTF_8));
			buf.release();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("server exceptionCaught.." + cause);
		ctx.close();
	}

//	@Override
//	public void channelReadComplete(ChannelHandlerContext ctx) {
//		ctx.flush();
//	}

	/**
	 * 设置HTTP返回头信息
	 */
	private void setHeaders(FullHttpResponse response, HttpRequest request) {
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=" + Charset.defaultCharset().name());
		response.headers().set(HttpHeaderNames.CONTENT_LANGUAGE, response.content().readableBytes());
		if (HttpUtil.isKeepAlive(request)) {
//			response.headers().set(HttpHeaderNames.CONNECTION, AsciiString.cached("keep-alive"));
			response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
	}

}
