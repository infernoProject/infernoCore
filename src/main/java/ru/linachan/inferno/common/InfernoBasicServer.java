package ru.linachan.inferno.common;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfernoBasicServer implements Runnable {

    private int serverPort;
    private InfernoChannelHandler channelHandler;

    private EventLoopGroup master = new NioEventLoopGroup();
    private EventLoopGroup worker = new NioEventLoopGroup();

    private static Logger logger = LoggerFactory.getLogger(InfernoBasicServer.class);

    public InfernoBasicServer(int bindPort) {
        serverPort = bindPort;

        channelHandler = new InfernoChannelHandler();
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

            ChannelFuture f = b.bind(serverPort).sync();
            logger.info("InfernoBasicServer started on {}", serverPort);

            f.channel().closeFuture().sync();
        } catch (InterruptedException ignored) {}
    }

    public InfernoChannelHandler channelHandler() {
        return channelHandler;
    }

    public void stop() {
        logger.info("InfernoBasicServer on {} is going to shutdown", serverPort);
        worker.shutdownGracefully();
        master.shutdownGracefully();
    }
}
