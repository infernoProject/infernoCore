package ru.infernoproject.tests.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.xor.XORCodec;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

import java.util.LinkedList;
import java.util.Queue;

public class TestClient extends SimpleChannelInboundHandler<ByteWrapper> {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private boolean connected = false;
    private Channel channel;

    private TestClientChannelHandler channelHandler = new TestClientChannelHandler();
    private final Queue<ByteWrapper> receiveQueue = new LinkedList<>();

    protected final Logger logger;

    public TestClient(String host, int port) {
        Bootstrap b = new Bootstrap();

        logger = LoggerFactory.getLogger(getClass());

        channelHandler.addHandler(XORCodec.class);
        channelHandler.addHandler(this);

        try {
            b.group(group)
                .channel(NioSocketChannel.class)
                .handler(channelHandler);

            channel = b.connect(host, port).sync()
                .channel();

            while (!channel.isOpen()) {
                Thread.sleep(100);
            }

            connected = true;
        } catch (InterruptedException e) {
            onInterrupt(e);
            Thread.currentThread().interrupt();
        }
    }

    private void onInterrupt(InterruptedException exc) {
        if (connected) {
            disconnect();
        }
    }

    public synchronized void send(ByteArray data) {
        try {
            channel.writeAndFlush(data).sync();
        } catch (InterruptedException e) {
            onInterrupt(e);
            Thread.currentThread().interrupt();
        }
    }

    public synchronized ByteWrapper recv(int retryCount, int timeOut) throws InterruptedException {
        for (int tryNumber = 0; tryNumber < retryCount; tryNumber++) {
            if (!receiveQueue.isEmpty())
                return receiveQueue.poll();

            Thread.sleep(timeOut);
        }

        return null;
    }

    public void disconnect() {
        group.shutdownGracefully();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteWrapper in) throws Exception {
        receiveQueue.add(in);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process response: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}