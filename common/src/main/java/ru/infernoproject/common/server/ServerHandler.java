package ru.infernoproject.common.server;

import io.netty.channel.*;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.auth.AccountManager;
import ru.infernoproject.common.auth.SessionManager;
import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.characters.CharacterManager;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.data.DataManager;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.jmx.InfernoMBean;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanOperation;
import ru.infernoproject.common.realmlist.RealmList;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.common.utils.ErrorUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.infernoproject.common.constants.CommonErrorCodes.*;

@ChannelHandler.Sharable
public abstract class ServerHandler extends ChannelInboundHandlerAdapter implements InfernoMBean {

    protected final DataSourceManager dataSourceManager;

    protected final RealmList realmList;

    protected final SessionManager sessionManager;
    protected final AccountManager accountManager;
    protected final CharacterManager characterManager;
    protected final DataManager dataManager;

    private final Map<SocketAddress, ServerSession> sessions;
    private final Map<Byte, Method> actions;

    protected static Logger logger;

    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors() * 10
    );

    public ServerHandler(DataSourceManager dataSourceManager, ConfigFile configFile) {
        logger = LoggerFactory.getLogger(getClass());

        this.dataSourceManager = dataSourceManager;

        this.realmList = new RealmList(dataSourceManager);

        this.sessionManager = new SessionManager(dataSourceManager, configFile);
        this.accountManager = new AccountManager(dataSourceManager, sessionManager, configFile);
        this.characterManager = new CharacterManager(dataSourceManager, configFile);
        this.dataManager = new DataManager(dataSourceManager);

        this.sessions = new ConcurrentHashMap<>();
        this.actions = new HashMap<>();

        registerActions();

        schedule(realmList::check, 10, 30);

        schedule(accountManager::cleanup, 10, 60);
        schedule(sessionManager::cleanup, 10, 60);
        schedule(characterManager::cleanup, 10, 60);
    }

    protected void schedule(ServerJob job, int delay, int period) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                job.run();
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            } catch (Exception e) {
                logger.error("Error:", e);
            }
        }, delay, period, TimeUnit.SECONDS);
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
                if (logger.isDebugEnabled())
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

        if (logger.isDebugEnabled())
            logger.debug("IN: {}", request.toString());

        ServerSession serverSession = sessionGet(ctx.channel().remoteAddress());
        byte opCode = request.getByte();

        if (actions.containsKey(opCode)) {
            Method actionMethod = actions.get(opCode);
            ServerAction serverAction = actionMethod.getAnnotation(ServerAction.class);

            if (!serverAction.authRequired()||(serverSession.isAuthorized()&&AccountLevel.hasAccess(serverSession.getAccount().accessLevel, serverAction.minLevel()))) {
                try {
                    response = (ByteArray) actionMethod.invoke(this, request, serverSession);
                } catch (InvocationTargetException e) {
                    ErrorUtils.logger(logger).error("Unable to process request", e);

                    response = new ByteArray(SERVER_ERROR);
                }
            } else {
                response = new ByteArray(AUTH_REQUIRED);
            }
        } else {
            response = new ByteArray(UNKNOWN_OPCODE);
        }

        if (logger.isDebugEnabled())
            logger.debug("OUT: {}", response.toString());

        sessionManager.update(serverSession.address());
        ctx.write(new ByteArray(opCode).put(response));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ErrorUtils.logger(logger).error("Unable to process request", cause);
        ctx.close();
    }

    @InfernoMBeanOperation(description = "Get number of connected users")
    public int getConcurrentConnectedUserCount() {
        return sessionList().size();
    }

    @InfernoMBeanOperation(description = "List IP and login of connected users")
    public Map<String, String> getConcurrentConnectedUsers() {
        return sessionList().stream()
            .collect(Collectors.toMap(
                serverSession -> ((InetSocketAddress) serverSession.address()).getHostName(),
                serverSession -> serverSession.getAccount() != null ? serverSession.getAccount().login : "UNAUTHORIZED"
            ));
    }

    public ServerSession sessionGet(SocketAddress remoteAddress) {
        return sessions.getOrDefault(remoteAddress, null);
    }

    public List<ServerSession> sessionList() {
        return new ArrayList<>(sessions.values());
    }

    protected abstract ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress);
    protected abstract void onSessionClose(SocketAddress remoteAddress);

    protected abstract void onShutdown();

    public void shutdown() {
        onShutdown();

        scheduler.shutdown();
    }
}
