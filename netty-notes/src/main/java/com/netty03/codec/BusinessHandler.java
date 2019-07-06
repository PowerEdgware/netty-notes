package com.netty03.codec;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.netty03.ProviderAnnotationScanner;
import com.netty03.annotation.ServiceProvider;
import com.netty03.message.MessageRequest;
import com.netty03.message.MessageResponse;
import com.netty03.message.RpcRequest;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class BusinessHandler extends ChannelInboundHandlerAdapter {

	private final ProviderAnnotationScanner scanner;
	// provider instance map
	private ConcurrentMap<String, Object> instanceMap = new ConcurrentHashMap<>(16);

	public BusinessHandler() {
		scanner = new ProviderAnnotationScanner();
		Set<Class<?>> providerClasses = scanner.scannerClass("com.netty03");

		instantiateAndRegisterProvider(providerClasses);
		System.out.println("BusinessHandler register provider suc.providers=" + instanceMap);
	}

	private void instantiateAndRegisterProvider(Set<Class<?>> providerClasses) {
		if (providerClasses.isEmpty()) {
			throw new IllegalArgumentException("no providers");
		}
		providerClasses.forEach(item -> {
			ServiceProvider sProvider = item.getAnnotation(ServiceProvider.class);
			if (sProvider != null) {
				// 接口判断
				if (item.isInterface()) {
					throw new RuntimeException("class=" + item.getName() + " can not be interface!");
				}
				Class<?> pclasses[] = sProvider.classes();
				Class<?> Interfaces[] = item.getInterfaces();
				List<Class<?>> pclassesList = Arrays.asList(pclasses);
				List<Class<?>> InterfacesList = Arrays.asList(Interfaces);
				// 子集
				boolean b = InterfacesList.containsAll(pclassesList);
				if (!b) {
					throw new RuntimeException("class:" + item.getName() + " ServiceProvider classes:" + pclasses
							+ " not all implements by:" + InterfacesList);
				}
				Object ItemInstance = null;
				try {
					ItemInstance = item.getDeclaredConstructor().newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e1) {
					e1.printStackTrace();
				}
				for (Class<?> IproviderClass : pclassesList) {
					if (ItemInstance == null) {
						break;
					}
					instanceMap.put(IproviderClass.getName(), ItemInstance);
				}
			}
		});

	}

	// 处理业务
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		boolean continuefireMsg = false;
		if (msg instanceof MessageRequest) {
			MessageRequest req = (MessageRequest) msg;
			RpcRequest request = req.getRequest();
			String className = request.getClassName();
			if (!instanceMap.containsKey(className)) {
				continuefireMsg = true;
			} else {
				// 处理消息
				Object providerInstance = instanceMap.get(request.getClassName());
				Method method = providerInstance.getClass().getMethod(request.getMethodName(), request.getParames());
				Object result = method.invoke(providerInstance, request.getValues());
				
				MessageResponse response=new MessageResponse();
				response.setMsgId(req.getMsgId());
				response.setResponse(result);
				
				System.out.println(providerInstance.getClass().getName() + " handle result=" + result);
				if (result != null) {
					ctx.writeAndFlush(response);
				}
			}
		}
		// 继续传播消息
		if (continuefireMsg)
			ctx.fireChannelRead(msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		cause.printStackTrace();
	}
}
