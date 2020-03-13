package com.netty.bytebuf;

public class RuntimeDemo {

	public static void main(String[] args) {
		System.out.println(Runtime.getRuntime().maxMemory());
		System.out.println(java.lang.Runtime.getRuntime().availableProcessors());
		//PoolChunk
	}
}
