package com.netty03.message;

import java.io.Serializable;

public class MessageResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5945663958172163606L;

	private String msgId;
	private Object response;
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public Object getResponse() {
		return response;
	}
	public void setResponse(Object response) {
		this.response = response;
	}
	
	
}
