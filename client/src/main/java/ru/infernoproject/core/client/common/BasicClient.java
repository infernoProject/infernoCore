package ru.infernoproject.core.client.common;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.core.common.codec.xor.XORDecoder;
import ru.infernoproject.core.common.codec.xor.XOREncoder;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;
import ru.infernoproject.core.common.utils.Callback;

import java.util.HashMap;
import java.util.Map;

public abstract class BasicClient extends SimpleChannelInboundHandler<ByteWrapper> {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private boolean connected = false;
    private Channel channel;

    private ClientChannelHandler channelHandler = new ClientChannelHandler();
    private final Map<String, Callback> callbacks = new HashMap<>();

    protected static final Logger logger = LoggerFactory.getLogger(BasicClient.class);

    public BasicClient(String host, int port) {

        Bootstrap b = new Bootstrap();

        channelHandler.addHandler(XORDecoder.class);
        channelHandler.addHandler(XOREncoder.class);
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
        }
    }

    private void onInterrupt(InterruptedException exc) {
        logger.info("Execution interrupted.");

        if (connected) {
            disconnect();
        }
    }

    public void send(ByteConvertible byteConvertible) {
        send(byteConvertible.toByteArray());
    }

    public synchronized void send(byte... data) {
        logger.debug(String.format("OUT: %s", HexBin.encode(data)));

        try {
            channel.writeAndFlush(data).sync();
        } catch (InterruptedException e) {
            onInterrupt(e);
        }
    }

    public void disconnect() {
        group.shutdownGracefully();
    }

    protected void registerCallBack(String name, Callback callBack) {
        callbacks.put(name, callBack);
    }

    protected Callback getCallBack(String name) {
        return callbacks.get(name);
    }

    public boolean isConnected() {
        return connected;
    }
}
