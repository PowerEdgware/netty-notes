package com.netty03.message;

public class RpcResponse {

	private int code=1;//suc or fail
	private Object response;
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public Object getResponse() {
		return response;
	}
	public void setResponse(Object response) {
		this.response = response;
	}
	
}
