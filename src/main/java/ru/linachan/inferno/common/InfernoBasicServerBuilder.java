package ru.linachan.inferno.common;


import io.netty.channel.ChannelHandler;

public class InfernoBasicServerBuilder {

    private InfernoBasicServer server;

    public InfernoBasicServerBuilder(int port) {
        server = new InfernoBasicServer(port);
    }

    public InfernoBasicServerBuilder addHandler(Class<? extends ChannelHandler> handler) {
        server.channelHandler().addHandler(handler);

        return this;
    }

    public InfernoBasicServerBuilder addHandler(ChannelHandler handler) {
        server.channelHandler().addHandler(handler);

        return this;
    }

    public InfernoBasicServer build() {
        return server;
    }
}