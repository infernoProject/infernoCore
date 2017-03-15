package ru.infernoproject.common.server;

import com.google.common.base.Joiner;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.auth.AccountManager;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.infernoproject.common.constants.ErrorCodes.UNKNOWN_OPCODE;

@ChannelHandler.Sharable
public abstract class ServerHandler extends ChannelInboundHandlerAdapter {

    protected final DataSourceManager dataSourceManager;

    protected final AccountManager accountManager;

    private final Map<SocketAddress, ServerSession> sessions;
    private final Map<Byte, Method> actions;

    protected static Logger logger;

    public ServerHandler(DataSourceManager dataSourceManager, AccountManager accountManager) {
        logger = LoggerFactory.getLogger(getClass());

        this.dataSourceManager = dataSourceManager;

        this.accountManager = accountManager;

        this.sessions = new ConcurrentHashMap<>();
        this.actions = new HashMap<>();

        registerActions();
    }

    private boolean validateAction(Method action) {
        return action.isAnnotationPresent(ServerAction.class) &&
            action.getReturnType().equals(ByteArray.class) &&
            action.getParameterCount() == 2 &&
            action.getParameterTypes()[0].equals(ByteWrapper.class) &&
            ServerSession.class.isAssignableFrom(action.getParameterTypes()[1]);
    }

    private void registerActions() {
        logger.info("Looking for ServerActions");
        for (Method method: getClass().getDeclaredMethods()) {
            if (!validateAction(method))
                continue;

            ServerAction serverAction = method.getAnnotation(ServerAction.class);
            for (byte opCode: serverAction.opCode()) {
                logger.debug(String.format("Action(0x%02X): %s", opCode, method.getName()));
                actions.put(opCode, method);
            }
        }
        logger.info("ServerActions registered: {}", actions.size());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        ServerSession serverSession = onSessionInit(ctx, remoteAddress);

        sessions.put(remoteAddress, serverSession);

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

        ServerSession serverSession = sessionGet(ctx.channel().remoteAddress());
        byte opCode = request.getByte();

        if (actions.containsKey(opCode)) {
            Method actionMethod = actions.get(opCode);

            response = (ByteArray) actionMethod.invoke(this, request, serverSession);
        } else {
            response = new ByteArray(UNKNOWN_OPCODE);
        }

        logger.debug("OUT: {}", response.toString());

        accountManager.sessionUpdateLastActivity(serverSession.address());
        ctx.write(new ByteArray(opCode).put(response).toByteArray());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getClass().equals(InvocationTargetException.class)) {
            Throwable exc = ((InvocationTargetException) cause).getTargetException();

            logger.error("Unable to process request: [{}]: {}", exc.getClass().getSimpleName(), exc.getMessage());
            logger.error(generateTrace(exc));
        } else {
            logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
            logger.error(generateTrace(cause));
        }
        ctx.close();
    }

    private String generateTrace(Throwable throwable) {
        List<String> sTraceStrings = new ArrayList<>();

        for (StackTraceElement sTrace: throwable.getStackTrace()) {
            sTraceStrings.add(String.format(
                "%s:%d - %s - %s",
                sTrace.getFileName(), sTrace.getLineNumber(),
                sTrace.getClassName(), sTrace.getMethodName()
            ));
        }

        return "StackTrace:\n\n\t" + Joiner.on("\n\t").join(sTraceStrings) + "\n\n";
    }

    protected ServerSession sessionGet(SocketAddress remoteAddress) {
        return sessions.getOrDefault(remoteAddress, null);
    }

    protected List<ServerSession> sessionList() {
        return sessions.values().stream().collect(Collectors.toList());
    }

    protected abstract ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress);
    protected abstract void onSessionClose(SocketAddress remoteAddress);
}
