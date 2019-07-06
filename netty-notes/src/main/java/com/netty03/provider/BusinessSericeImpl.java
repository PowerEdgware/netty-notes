package com.netty03.provider;

import java.util.Map;

import com.netty03.annotation.ServiceProvider;
import com.netty03.api.IBusinessService;

@ServiceProvider(classes = { IBusinessService.class })
public class BusinessSericeImpl implements IBusinessService {

	@Override
	public Object doBusiness(Map<String, String> params) {
		System.out.println("Recved request=" + params);

		// DO DB
		return "hello," + params;
	}

}
