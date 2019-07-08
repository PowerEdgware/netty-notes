package com.netty05;
//EventLoop与Pipeline

class Readme {

	void readme() {
		//EventLoop
		//NioEventLoop
		//Pipeline
		//DefaultChannelPipeline
		//ServerBootstrap.bind-->AbstractBootstrap.doBind->initAndRegister创建serversocketchannel-->ServerBootstrap.init初始化serverchannel在pipeline上添加handler-->AbstractChannel$AbstractUnsafe.register->AbstractUnsafe.register0（启动eventLoop轮询线程并处理register0）-->AbstractChannel.doRegister实现javaNIO的注册-->pipeline.fireChannelRegistered()注册事件的传播-->ChannelInitializer.channelRegistered->initChannel处理server端注册的handler（如果有的话），间接添加：ServerBootstrapAcceptor 连接接入处理器-->register内部 pipeline.fireChannelActive()激活channel准备接收连接接入-->DefultChannelPipeline$HeadContext.channelActive.readIfIsAutoRead()-->AbstractNioChannel.doBeginRead
		//ServerBootstrap$ServerBootstrapAcceptor.channelRead
		//
		
		
	}
}