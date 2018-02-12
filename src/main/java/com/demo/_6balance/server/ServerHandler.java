
package com.demo._6balance.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Date;

/**
 * 处理与客户端之间的连接
 * 客户端连接断开等触发此类
 * 
 * @author jerome_s@qq.com
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {

	private final BalanceUpdateProvider balanceUpdater;
	/** 负载均衡累加数值 */
	private static final Integer BALANCE_STEP = 1;

	public ServerHandler(BalanceUpdateProvider balanceUpdater) {
		this.balanceUpdater = balanceUpdater;
	}

	public BalanceUpdateProvider getBalanceUpdater() {
		return balanceUpdater;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("one client connect...");
		balanceUpdater.addBalance(BALANCE_STEP);
		ByteBuf resp = Unpooled.copiedBuffer("welcome!".getBytes());
		ctx.writeAndFlush(resp);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("one client disconnect...");
		balanceUpdater.reduceBalance(BALANCE_STEP);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("server channelRead..");
		ByteBuf buf = (ByteBuf) msg;
		byte[] req = new byte[buf.readableBytes()];
		buf.readBytes(req);
		String body = new String(req, "UTF-8");
		System.out.println("The time server receive order:" + body);
		String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(
				System.currentTimeMillis()).toString() : "BAD ORDER";
		ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
		ctx.write(resp);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		System.out.println("server channelReadComplete..");
		ctx.flush();//刷新后才将数据发出到SocketChannel
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
