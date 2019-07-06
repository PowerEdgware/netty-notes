package com.netty03.message;

import java.io.Serializable;

import lombok.Data;

//@Data
public class MessageRequest implements Serializable {

	/**
		 * 
		 */
	private static final long serialVersionUID = -5896307198984573167L;
	private String msgId;// 业务ID
	private RpcRequest request;

	public RpcRequest getRequest() {
		return request;
	}

	public void setRequest(RpcRequest request) {
		this.request = request;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

}
