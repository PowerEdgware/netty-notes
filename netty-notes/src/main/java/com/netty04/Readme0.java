package com.netty04;

public class Readme0 {

	/**
	 * 
	 * 

**1、为什么都说Netty是高性能的RPC框架？**
*1.1 IO模型，netty支持NIO，异步非阻塞IO模型。*
IO非阻塞使得netty只使用单线程selector便可以实现多连接的管理(IO多路复用)，每个连接把自己感兴趣的事件注册到管理器上，再加上非阻塞，内核一旦有连接上有事件发生，便能唤醒selector线程，实现，多连接多事件的处理。再配合多线程模型，实现高并发高数量连接的请求处理和连接管理不再是难事(IO多路复用)

*1.2 线程模型：netty支持单线程，多线程模型，主从线程模型*
在1.1的基础上，单线程selector管理多个连接，处理效率不高，引入reactor多线程模型，主从线程模型，使得，对连接进行均衡处理，对连接的事件做到单一化，比如主线程(boss线程)管理连接的连接接入事件，从线程池(worker线程)管理连接的读写事件，做到职责单一化。在这种模式下(异步非阻塞+worker线程池对读写事件的业务处理+boss线程对介入的管理)最小化占有系统资源(原阻塞模式下，每一个连接要单独一个线程进行处理，连接和线程数成正比)，最大化连接管理。

*1.3 高效编解码方式(支持多种方式的序列化与反序列化)*
序列化和反序列化高效主要体现在cpu的计算时间和序列化后字节流的大小，时间越少，字节流越少效率越高。netty提供了多种编码器/解码器支持多种序列化与反序列化方式，比如protobuf等。

*1.4 事件消息的无锁串行处理*
netty为每个连接读写事件注册到单独的selector，而NIOEventloop的selector由单独的线程轮询处理。多个selector组成了worker线程池，使得每个单线程selector管理器均衡处理多个连接，每次只串行处理一个事件，该事件沿着每个连接对应的pipeline上传播，使得每个selector对应的IO线程对事件的处理都是单独的，无干扰，也就没有锁的竞争，在多核cpu下做到真正的并行处理。

*1.5 netty堆外内存池的使用(同时实现零拷贝)*
内存管理这块我不是很熟。大致说说我的理解，通常的Java应用对socketIO流数据的处理，先是从内核缓冲区拷贝数据到堆外内存，再从堆外拷贝到java程序的堆内存(heap)，**为什么操作系统不直接把内核数据拷贝到java堆内存?这个是因为堆内存分配与回收是java自己管理的，假设操作系统把数据拷贝到程序指定的堆内存位置，但是由于某个时刻堆进行了垃圾回收，那么操作系统再去按照先前分配的堆内存地址去写或读该数据区域，这个时候数据可能搬家了**
完了再处理读取到的数据，写的流程一样。有了这个铺垫，netty就自己实现了堆外内存的分配和释放（**操作系统对内存的管理很重要**），并使用内存池，重复利用内存，避免了内存的不断分配和释放所带来的系统开销。既然他能一次性分配一块大内存，那么他也可以实现在这个内存下的数据的组装和拆分而不必像传统方式占用内存拷贝时间(至于它的实现细节有待研究)。netty对文件的传输也实现了零拷贝。

**2、服务端的Socket在哪里开始初始化？**
ServerBootstrap.bind-->AbstractBootstrap.doBind->initAndRegister创建serversocketchannel-->ServerBootstrap.init初始化serverchannel在pipeline上添加handler-->AbstractChannel$AbstractUnsafe.register->AbstractUnsafe.register0（启动eventLoop轮询线程并处理register0）-->AbstractChannel.doRegister实现javaNIO的注册-->pipeline.fireChannelRegistered()注册事件的传播-->ChannelInitializer.channelRegistered->initChannel处理server端注册的handler（如果有的话），间接添加：ServerBootstrapAcceptor 连接接入处理器-->register内部 pipeline.fireChannelActive()激活channel准备接收连接接入-->DefultChannelPipeline$HeadContext.channelActive.readIfIsAutoRead()-->AbstractNioChannel.doBeginRead
```java
@Override
    protected void doBeginRead() throws Exception {
        // Channel.read() or ChannelHandlerContext.read() was called
        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            return;
        }

        readPending = true;

        final int interestOps = selectionKey.interestOps();
        if ((interestOps & readInterestOp) == 0) {
           ** selectionKey.interestOps(interestOps | readInterestOp);**
        }
    }

```


3、服务端的Socket在哪里开始accept链接？
NioEventLoop.run->processSelectedKeys->processSelectedKeysOptimized->processSelectedKey->AbstractNioMessageChannel$NioMessageUnsafe.read->NioServerSocketChannel.doReadMessages
代码：
```
 @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }
```
	 */
}
