package com.netty02;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DirectBufferDemo {

	void init() {
		// DirectByteBuffer
		// ByteBuffer
		// VM
		// Bits
		//System.initPhase1()

	}

	public static void main(String[] args) {

		int i = 3;// i << 0
		System.out.println(i << 0);
		System.out.println(Runtime.getRuntime().maxMemory()+"  maxMemory");

		ByteBuffer buffer = ByteBuffer.allocateDirect(128);
		// buffer.array()
		buffer.put("abc".getBytes());
		buffer.flip();
		while (buffer.hasRemaining()) {
			byte b = buffer.get();
			System.out.println(b);
		}
		buffer.clear();
		buffer=null;
		// ByteBuffer.wrap(array)

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
