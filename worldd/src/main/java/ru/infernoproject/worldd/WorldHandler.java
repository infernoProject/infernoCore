package ru.infernoproject.worldd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.worldd.world.MovementInfo;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.map.MapManager;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import java.net.SocketAddress;
import java.sql.*;

import static ru.infernoproject.worldd.constants.WorldErrorCodes.*;
import static ru.infernoproject.worldd.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final MapManager mapManager;
    private final String serverName;

    public WorldHandler(DataSourceManager dataSourceManager, ConfigFile config) {
        super(dataSourceManager, config);

        mapManager = new MapManager();
        serverName = config.getString("world.name", null);

        if (serverName == null) {
            logger.error("Server name not specified");
            System.exit(1);
        }

        try {
            if (!realmList.exists(serverName)) {
                logger.error("Server with name '{}' is not registered", serverName);
                System.exit(1);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            System.exit(1);
        }

        schedule(() -> realmList.online(serverName, true), 10, 15);
    }

    @Override
    protected ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        return new WorldSession(ctx, remoteAddress);
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {
        try {
            sessionManager.kill(sessionGet(remoteAddress).getAccount());
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
        }
    }

    @ServerAction(opCode = AUTHORIZE)
    public ByteArray authorize(ByteWrapper request, ServerSession serverSession) {
        try {
            Session session = sessionManager.get(request.getBytes());
            Account account = sessionManager.authorize(session, serverSession.address());

            if (account != null) {
                serverSession.setAuthorized(true);
                serverSession.setAccount(account);

                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(AUTH_ERROR);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = {
        MOVE_START_FORWARD, MOVE_START_BACKWARD, MOVE_STOP, MOVE_START_STRAFE_LEFT, MOVE_START_STRAFE_RIGHT,
        MOVE_STOP_STRAFE, MOVE_JUMP, MOVE_START_TURN_LEFT, MOVE_START_TURN_RIGHT, MOVE_STOP_TURN,
        MOVE_START_PITCH_UP, MOVE_START_PITCH_DOWN, MOVE_STOP_PITCH, MOVE_SET_RUN_MODE, MOVE_SET_WALK_MODE,
        MOVE_FALL_LAND, MOVE_START_SWIM, MOVE_STOP_SWIM, MOVE_SET_FACING, MOVE_SET_PITCH
    })
    public ByteArray move(ByteWrapper request, ServerSession session) {
        request.rewind();

        byte opCode = request.getByte();
        MovementInfo move = MovementInfo.read(request);

        if (move.validatePosition()) {
            return new ByteArray(INVALID_REQUEST);
        }

        if (opCode == MOVE_FALL_LAND) {
            ((WorldSession) session).getPlayer().handleFall(move);
        }

        ((WorldSession) session).getPlayer().handleMove(opCode, move);

        return new ByteArray(SUCCESS);
    }

    @ServerAction(opCode = LOG_OUT)
    public ByteArray logOut(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray(AUTH_REQUIRED);

        try {
            sessionManager.kill(session.getAccount());

            return new ByteArray(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = HEART_BEAT)
    public ByteArray heartBeat(ByteWrapper request, ServerSession session) {
        return new ByteArray(SUCCESS).put(request.getLong());
    }

    public void update(Long diff) {
        mapManager.update(diff);

        sessionList().parallelStream().forEach(session -> {
            WorldPlayer player = ((WorldSession) session).getPlayer();

            if (player != null) {
                player.update(diff);
            }
        });
    }
}
