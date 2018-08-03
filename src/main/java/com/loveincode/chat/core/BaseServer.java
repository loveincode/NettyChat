package com.loveincode.chat.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseServer implements Server {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	protected String host = "localhost";
	protected int port = 9090;

	protected DefaultEventLoopGroup defLoopGroup;
	//Boss 线程池，内部维护了一组线程，每个线程负责处理多个Channel上的事件，而一个Channel只对应于一个线程，这样可以回避多线程下的数据同步问题。
	protected NioEventLoopGroup bossGroup;
	protected NioEventLoopGroup workGroup;
	protected NioServerSocketChannel ssch;
	protected ChannelFuture cf;
	//服务端做配置和启动的类。
	protected ServerBootstrap b;

	public void init() {
		defLoopGroup = new DefaultEventLoopGroup(8, new ThreadFactory() {
			private AtomicInteger index = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "DEFAULTEVENTLOOPGROUP_" + index.incrementAndGet());
			}
		});
		bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
			private AtomicInteger index = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "BOSS_" + index.incrementAndGet());
			}
		});
		workGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 10, new ThreadFactory() {
			private AtomicInteger index = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "WORK_" + index.incrementAndGet());
			}
		});

		b = new ServerBootstrap();
	}

	@Override
	public void shutdown() {
		if (defLoopGroup != null) {
			defLoopGroup.shutdownGracefully();
		}
		bossGroup.shutdownGracefully();
		workGroup.shutdownGracefully();
	}
}
