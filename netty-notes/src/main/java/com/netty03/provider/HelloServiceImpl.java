package com.netty03.provider;

import com.netty03.annotation.ServiceProvider;
import com.netty03.api.IHelloService;

@ServiceProvider(classes = { IHelloService.class })
public class HelloServiceImpl implements IHelloService {

	@Override
	public String sayHello(String name) {
		return "hello," + name;
	}

}
