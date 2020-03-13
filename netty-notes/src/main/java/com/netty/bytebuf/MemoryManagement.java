package com.netty.bytebuf;


public class MemoryManagement {

	void readme() {
		//PoolChunk
		//PooledByteBufAllocator
		//PoolArena
		//1110 0000 0000
		//HashMap
		//TreeMap
		
	}
	public static void main(String[] args) {
//		System.out.println(0x0E00);
		int reqCap=818;//Thread ThreadPoolExecutor
		System.out.println(Integer.toBinaryString(reqCap));
		System.out.println(normalCap(reqCap));
		System.out.println(3<<2);
	}
	
	
	static 	int normalCap(int reqCapacity) {
		   int normalizedCapacity = reqCapacity;
           normalizedCapacity --;
           normalizedCapacity |= normalizedCapacity >>>  1;
           normalizedCapacity |= normalizedCapacity >>>  2;
           normalizedCapacity |= normalizedCapacity >>>  4;
           normalizedCapacity |= normalizedCapacity >>>  8;
           normalizedCapacity |= normalizedCapacity >>> 16;
           normalizedCapacity ++;

           if (normalizedCapacity < 0) {
               normalizedCapacity >>>= 1;
           }
           return normalizedCapacity;
	}
	
}
