package ru.linachan.inferno.common;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InfernoChannelHandler extends ChannelInitializer<SocketChannel> {

    private List<Object> handlers = new CopyOnWriteArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public void initChannel(SocketChannel channel) throws Exception {
        for (Object channelHandler: handlers) {
            if (channelHandler instanceof ChannelHandler) {
                channel.pipeline().addLast((ChannelHandler) channelHandler);
            } else if (channelHandler instanceof Class) {
                channel.pipeline().addLast(((Class<? extends ChannelHandler>) channelHandler).newInstance());
            }
        }
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
