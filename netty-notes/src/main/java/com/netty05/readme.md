1、了解Netty服务端的线程池分配规则，线程何时启动。
服务端boss线程分配一个就行，分配多了也不起作用，因为他是每个端口绑定一个线程。worker线程可以多个。
首先来一张截图：
netty_boss_worker.png

显示了boss配置多个，worker也配置cpu核心数*4 的场景，这里我用了自定义线程池方便查看boss-worker线程。可以看到，只有一个boss线程被启动了，其他的没用到。

boss线程启动时机：
ServerBootstrap.bind–>AbstractBootstrap.doBind->initAndRegister创建serversocketchannel–>ServerBootstrap.init初始化serverchannel在pipeline上添加handler–>AbstractChannel$AbstractUnsafe.register->AbstractUnsafe.register0（启动eventLoop轮询线程并处理register0）
到了这里开始注册：

  @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            AbstractChannel.this.eventLoop = eventLoop;

            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {//这里执行注册register0 里面启动了线程
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

eventloop.execute方法调用SingleThreadEventExecutor.execute：

 @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        boolean inEventLoop = inEventLoop();
        addTask(task);
        if (!inEventLoop) {
            startThread();
            if (isShutdown() && removeTask(task)) {
                reject();
            }
        }

        if (!addTaskWakesUp && wakesUpForTask(task)) {
            wakeup(inEventLoop);
        }
    }
startThread()–>doStartThread():
doStartThread调用：ThreadPerTaskExecutor.execute()创建一个线程开始执行NioEventLoop的run方法，此方法开始了selector死循环轮询过程。

worker线程启动时机：
worker在服务端启动完毕时是没有启动的，上图是我建立客户端连接后的截图，所以worker也被启动了。也就是worker线程的启动是由连接上来时启动的。具体流程：

NioEventLoop.run->processSelectedKeys->processSelectedKeysOptimized->processSelectedKey方法：

 // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();//连接事件进来：这里调用AbstractNioMessageChannel$NioMessageUnsafe.read方法
            }
AbstractNioMessageChannel$NioMessageUnsafe.read->NioServerSocketChannel.doReadMessages返回后继续调用read后续方法，事件沿着Pipeleine传播：pipeline.fireChannelRead
沿着head传播到：ServerBootstrap$ServerBootstrapAcceptor.channelRead

 @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;

            child.pipeline().addLast(childHandler);

            setChannelOptions(child, childOptions, logger);

            for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }

            try {
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }
调用了childGroup.register，到这里和服务端boss注册一样，会在register0时启动childGroup内的一个NioEventLoop线程。其他线程启动一样。

2、了解Netty是如何解决JDK空轮训Bug的？
selector在select的时候，select(boolean oldWakenUp)会在1s内检测没有事件发生时轮询的次数，如果1s内轮询次数超过
阈值：512 且没有任何事件发生，就会重建selector.

else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                        selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                            selectCnt, selector);

                    rebuildSelector();//这里发生selector重新构建
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }
阈值默认：512 但是可以通过系统变量改变：

   int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;
重建selector 就是把原selector上注册的channel以及事件全部搬到新的selector上面并关闭旧的selector管理器。

3、Netty是如何实现异步串行无锁化编程的？
内核epoll的支持，每个连接都会有对应的pipeline，pipeline上对应着编解码处理器，每一个连接都会被绑定到一个EventLoop线程之上，且只绑定一个。但一个EventLoop可以管理多个连接channel.读写事件到来的时候，绑定到同一个eventLoop的channel处理流程是单线程串行的,读写事件沿着pipeline传播，直到消息的处理，该流程没有任何锁的竞争，在多核心cup下实现channel事件真正的并行处理。