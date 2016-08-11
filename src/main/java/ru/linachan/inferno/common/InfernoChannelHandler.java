package ru.linachan.inferno.common;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InfernoChannelHandler extends ChannelInitializer<SocketChannel> {

    private List<Object> handlers = new CopyOnWriteArrayList<>();
    private Map<SocketAddress, SocketChannel> channels = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void initChannel(SocketChannel channel) throws Exception {
        SocketAddress remoteAddress = channel.remoteAddress();
        channels.put(remoteAddress, channel);

        for (Object channelHandler: handlers) {
            if (channelHandler instanceof ChannelHandler) {
                channel.pipeline().addLast((ChannelHandler) channelHandler);
            } else if (channelHandler instanceof Class) {
                channel.pipeline().addLast(((Class<? extends ChannelHandler>) channelHandler).newInstance());
            }
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        channels.remove(remoteAddress);

        super.channelUnregistered(ctx);
    }

    public void addHandler(Class<? extends ChannelHandler> handler) {
        handlers.add(handler);
    }

    public void addHandler(ChannelHandler handler) {
        handlers.add(handler);
    }

    public void resetHandlers() {
        handlers.clear();
    }
}
