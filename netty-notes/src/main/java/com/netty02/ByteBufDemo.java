package com.netty02;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;

public class ByteBufDemo {
	
	void init() {
		//DirectByteBuffer
		//ByteBuffer
		//VM
		//Bits
		
	}

	public static void main(String[] args) {
		// nio read
		readFile();

		nioWrite();

		// file map
		try {
			RandomAccessFile arf = new RandomAccessFile("files/niomap.txt", "rw");
			FileChannel fc = arf.getChannel();

			MappedByteBuffer mbbf = fc.map(MapMode.READ_WRITE, 0, 64);

			// 对映射缓冲区的修改就是修file
			mbbf.put(0, (byte) 97);
			mbbf.put(62, (byte) 98);

			// read content
			ByteBuffer buf = ByteBuffer.allocate(1024);
			fc.read(buf);

			buf.flip();
			System.out.println(new StringBuffer(Charset.defaultCharset().decode(buf)).toString());
			arf.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void nioWrite() {
		try {
			FileOutputStream fos = new FileOutputStream("files/nioWrite.txt");
			FileChannel fc = fos.getChannel();

			byte[] data = new String("demotxt").getBytes();
			ByteBuffer buf = ByteBuffer.wrap(data);

			int size = fc.write(buf);

			System.out.println("write len=" + size + " to file");

			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readFile() {
		try {
			FileInputStream fis = new FileInputStream("files/demo.txt");
			FileChannel fileChannel = fis.getChannel();
			ByteBuffer buf = ByteBuffer.allocate(512);
			// to buf
			int size = fileChannel.read(buf);

			buf.flip();// 切换读写模式
			// read from buf
			byte[] data = new byte[size];
			buf.get(data);
			System.out.println(new String(data, 0, size));
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
