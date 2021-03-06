package com.demo._6balance.server;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class ServerImpl implements Server {

	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workGroup = new NioEventLoopGroup();
	private ServerBootstrap bootStrap = new ServerBootstrap();
	private ChannelFuture channelFuture;
	private boolean SSL=false;

	private String zkAddress;
	private String serversPath;
	/** 当前节点路径 */
	private String currentServerPath;
	private ServerData serverData;

	private volatile boolean binded = false;

	private final ZkClient zkClient;
	private final RegistProvider registProvider;

	private static final Integer SESSION_TIME_OUT = 10000;
	private static final Integer CONNECT_TIME_OUT = 10000;

	public String getCurrentServerPath() {
		return currentServerPath;
	}

	public String getZkAddress() {
		return zkAddress;
	}

	public String getServersPath() {
		return serversPath;
	}

	public ServerData getSd() {
		return serverData;
	}

	public void setSd(ServerData sd) {
		this.serverData = sd;
	}

	public ServerImpl(boolean SSL,String zkAddress, String serversPath, ServerData serverData) {
		this.SSL = SSL;
		this.zkAddress = zkAddress;
		this.serversPath = serversPath;
		this.zkClient = new ZkClient(this.zkAddress, SESSION_TIME_OUT, CONNECT_TIME_OUT, new SerializableSerializer());
		this.registProvider = new DefaultRegistProvider();
		this.serverData = serverData;
	}

	/**
	 * 初始化服务端(注册节点到zookeeper)
	 * 
	 * @author jerome_s@qq.com
	 * @throws Exception
	 */
	private void initRunning() throws Exception {
		String mePath = serversPath.concat("/").concat(serverData.getPort().toString());
		System.out.println("Regist path="+mePath);
		// 注册到zookeeper
		registProvider.regist(new ZooKeeperRegistContext(mePath, zkClient, serverData));
		currentServerPath = mePath;
	}

	/**
	 * 绑定端口
	 */
	public void bind() throws CertificateException, SSLException {

		if (binded) {
			return;
		}

		System.out.println(serverData.getPort() + ":binding...");

		try {
			initRunning();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} else {
			sslCtx = null;
		}
		bootStrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc()));
						}
						p.addLast(new ServerHandler(new DefaultBalanceUpdateProvider(currentServerPath, zkClient)));
					}
				});

		try {
			channelFuture = bootStrap.bind(serverData.getPort()).sync();
			binded = true;
			System.out.println(serverData.getPort() + ":binded...");
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}

	}

}
