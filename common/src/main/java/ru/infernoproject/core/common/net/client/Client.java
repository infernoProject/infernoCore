package ru.infernoproject.core.common.net.client;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class Client extends SimpleChannelInboundHandler<ByteWrapper> {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private boolean connected = false;
    private Channel channel;

    private ClientChannelHandler channelHandler = new ClientChannelHandler();

    private final Map<String, Callback> actionCallBacks = new HashMap<>();
    private final Map<Byte, Method> callBacks;

    protected final Logger logger;

    public Client(String host, int port) {
        Bootstrap b = new Bootstrap();

        logger = LoggerFactory.getLogger(getClass());

        channelHandler.addHandler(XORDecoder.class);
        channelHandler.addHandler(XOREncoder.class);
        channelHandler.addHandler(this);

        callBacks = new HashMap<>();

        registerCallBacks();

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

    private boolean validateCallBack(Method action) {
        return action.isAnnotationPresent(ClientCallBack.class) &&
            action.getReturnType().equals(void.class) &&
            action.getParameterCount() == 1 &&
            action.getParameterTypes()[0].equals(ByteWrapper.class);
    }

    private void registerCallBacks() {
        logger.info("Looking for ClientCallBacks");
        for (Method method: getClass().getDeclaredMethods()) {
            if (!validateCallBack(method))
                continue;

            ClientCallBack clientCallBack = method.getAnnotation(ClientCallBack.class);
            logger.info(String.format("Action(0x%02X): %s", clientCallBack.opCode(), method.getName()));
            callBacks.put(clientCallBack.opCode(), method);
        }
        logger.info("ClientCallBacks registered: {}", callBacks.size());
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

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteWrapper in) throws Exception {
        logger.debug("IN: {}", in);

        Byte opCode = in.getByte();

        if (callBacks.containsKey(opCode)) {
            Method callBack = callBacks.get(opCode);

            callBack.invoke(this, in.getWrapper());
        } else {
            logger.info(String.format("Unexpected OpCode: 0x%02X", opCode));
        }
    }

    protected void registerCallBack(String name, Callback callBack) {
        actionCallBacks.put(name, callBack);
    }

    protected Callback getCallBack(String name) {
        return actionCallBacks.get(name);
    }

    public boolean isConnected() {
        return connected;
    }
}
