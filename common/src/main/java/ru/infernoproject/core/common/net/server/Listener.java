package ru.infernoproject.core.common.net.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener implements Runnable {

    private String serverHost;
    private int serverPort;

    private ChannelHandler channelHandler;

    private EventLoopGroup master = new NioEventLoopGroup();
    private EventLoopGroup worker = new NioEventLoopGroup();

    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    public Listener(String bindHost, int bindPort) {
        serverHost = bindHost;
        serverPort = bindPort;

        channelHandler = new ChannelHandler();
    }

    public static class Builder {

        private Listener listener;

        public Builder(String bindHost, int bindPort) {
            listener = new Listener(bindHost, bindPort);
        }

        public Builder addHandler(Class<? extends io.netty.channel.ChannelHandler> handler) {
            listener.channelHandler().addHandler(handler);

            return this;
        }

        public Builder addHandler(io.netty.channel.ChannelHandler handler) {
            listener.channelHandler().addHandler(handler);

            return this;
        }

        public Listener build() {
            return listener;
        }
    }

    @Override
    public void run() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(master, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelHandler)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(serverHost, serverPort).sync();
            logger.info("Listener started on {}:{}", serverHost, serverPort);

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public ChannelHandler channelHandler() {
        return channelHandler;
    }

    public void stop() {
        logger.info("Listener on {} is going to shutdown", serverPort);
        worker.shutdownGracefully();
        master.shutdownGracefully();
    }
}
