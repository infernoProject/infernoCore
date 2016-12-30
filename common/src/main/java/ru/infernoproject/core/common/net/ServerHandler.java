package ru.infernoproject.core.common.net;

import io.netty.channel.*;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteWrapper;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import static ru.infernoproject.core.common.constants.ErrorCodes.UNKNOWN_OPCODE;

@ChannelHandler.Sharable
public abstract class ServerHandler extends ChannelInboundHandlerAdapter {

    protected final DataSourceManager dataSourceManager;
    private final Map<SocketAddress, ServerSession> sessions;
    private final Map<Byte, Method> actions;

    protected static Logger logger;

    public ServerHandler(DataSourceManager dataSourceManager) {
        logger = LoggerFactory.getLogger(getClass());

        this.dataSourceManager = dataSourceManager;
        this.sessions = new HashMap<>();
        this.actions = new HashMap<>();

        registerActions();
    }

    private void registerActions() {
        logger.info("Looking for ServerActions");
        for (Method method: getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ServerAction.class)) {
                ServerAction serverAction = method.getAnnotation(ServerAction.class);
                logger.info(String.format("Action(0x%02X): %s", serverAction.opCode(), method.getName()));
                actions.put(serverAction.opCode(), method);
            }
        }
        logger.info("ServerActions registered: {}", actions.size());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();

        sessions.put(remoteAddress, onSessionInit(remoteAddress));

        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();

        onSessionClose(remoteAddress);
        sessions.remove(remoteAddress);

        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteWrapper request = (ByteWrapper) msg;
        ByteArray response;

        logger.debug("IN: {}", request.toString());

        ServerSession session = getSession(ctx.channel().remoteAddress());
        byte opCode = request.getByte();

        if (actions.containsKey(opCode)) {
            Method actionMethod = actions.get(opCode);

            response = (ByteArray) actionMethod.invoke(this, request, session);
        } else {
            response = new ByteArray().put(UNKNOWN_OPCODE);
        }

        logger.debug("OUT: {}", response.toString());

        session.update();
        ctx.write(new ByteArray().put(opCode).put(response).toByteArray());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }

    protected ServerSession getSession(SocketAddress remoteAddress) {
        return sessions.getOrDefault(remoteAddress, null);
    }

    protected abstract ServerSession onSessionInit(SocketAddress remoteAddress);
    protected abstract void onSessionClose(SocketAddress remoteAddress);
}
