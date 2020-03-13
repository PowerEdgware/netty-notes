package com.netty.bytebuf;

 class ByteBufStudy {
	void readme() {
//	     内存   ByteBuf
		// memory allocator
		// PooledByteBufAllocator
//	        ByteBufAllocator
		// PoolArena
		// CompositeByteBuf
//	        Unpooled
//	        UnpooledByteBufAllocator
		
		//内存分配器  ByteBufAllocator
		//AbstractByteBufAllocator
		
		//chunk page
		//PoolChunk
		//PoolArena
	}
	
	public static void main(String[] args) {
		System.out.println(Runtime.getRuntime().maxMemory());
		System.out.println(java.lang.Runtime.getRuntime().availableProcessors());
	}
}