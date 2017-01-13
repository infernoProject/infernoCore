package ru.infernoproject.core.worldd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.core.common.auth.AccountManager;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.net.server.ServerAction;
import ru.infernoproject.core.common.net.server.ServerHandler;
import ru.infernoproject.core.common.net.server.ServerSession;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.types.auth.Session;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteWrapper;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.worldd.characters.CharacterManager;
import ru.infernoproject.core.worldd.commands.WorldCommand;
import ru.infernoproject.core.worldd.commands.WorldCommandResult;
import ru.infernoproject.core.worldd.commands.WorldCommander;
import ru.infernoproject.core.worldd.commands.impl.AccountCommand;
import ru.infernoproject.core.worldd.commands.impl.HelpCommand;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;
import ru.infernoproject.core.worldd.data.WorldDataManager;
import ru.infernoproject.core.worldd.map.MapManager;

import java.net.SocketAddress;
import java.sql.*;
import java.util.List;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final WorldCommander commander;

    private final CharacterManager characterManager;
    private final WorldDataManager dataManager;

    private final MapManager mapManager;

    public WorldHandler(DataSourceManager dataSourceManager, AccountManager accountManager) {
        super(dataSourceManager, accountManager);

        commander = new WorldCommander();

        characterManager = new CharacterManager(dataSourceManager);
        dataManager = new WorldDataManager(dataSourceManager);

        mapManager = new MapManager();

        registerCommands();
    }

    private void registerCommands() {
        commander.register(HelpCommand.class);
        commander.register(AccountCommand.class);
    }

    @Override
    protected ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        return new WorldSession(ctx, remoteAddress);
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {
        try {
            accountManager.sessionKill(sessionGet(remoteAddress).getAccount());
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
        }
    }

    @ServerAction(opCode = AUTHORIZE)
    public ByteArray authorize(ByteWrapper request, ServerSession serverSession) {

        try {
            Session session = accountManager.sessionGet(request.getBytes());
            Account account = accountManager.sessionAuthorize(session, serverSession.address());

            if (account != null) {
                serverSession.setAuthorized(true);
                serverSession.setAccount(account);

                return new ByteArray().put(SUCCESS);
            } else {
                return new ByteArray().put(AUTH_ERROR);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = EXECUTE)
    public ByteArray commandExecute(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            String command = request.getString();
            String[] arguments = request.getStrings();

            WorldCommand commandInstance = commander.getCommand(command, session.getAccount().getAccessLevel());
            if (commandInstance != null) {
                WorldCommandResult result = commandInstance.execute(dataSourceManager, (WorldSession) session, arguments);

                return new ByteArray().put(SUCCESS).put(result);
            } else {
                return new ByteArray().put(UNKNOWN_COMMAND);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = CHARACTER_LIST)
    public ByteArray characterListGet(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            try {
                List<CharacterInfo> characterInfoList = characterManager.characterList((WorldSession) session);

                return new ByteArray().put(SUCCESS).put(characterInfoList);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = CHARACTER_CREATE)
    public ByteArray characterCreate(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            try {
                CharacterInfo characterInfo = characterManager.characterCreate(
                    new CharacterInfo(request.getWrapper()), (WorldSession) session
                );

                if (characterInfo == null) {
                    return new ByteArray().put(ALREADY_EXISTS);
                }

                return new ByteArray().put(SUCCESS).put(characterInfo);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = CHARACTER_SELECT)
    public ByteArray characterSelect(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            int characterId = request.getInt();

            try {
                CharacterInfo characterInfo = characterManager.characterGet(characterId, (WorldSession) session);

                if (characterInfo != null) {
                    return new ByteArray().put(SUCCESS).put(characterInfo);
                }

                return new ByteArray().put(CHARACTER_NOT_EXISTS);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = RACE_LIST)
    public ByteArray raceListGet(ByteWrapper request, ServerSession session) {
        try {
            List<RaceInfo> raceList = dataManager.raceList();

            return new ByteArray().put(SUCCESS).put(raceList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = CLASS_LIST)
    public ByteArray classListGet(ByteWrapper request, ServerSession session) {
        try {
            List<ClassInfo> classList = dataManager.classList();

            return new ByteArray().put(SUCCESS).put(classList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = LOG_OUT)
    public ByteArray logOut(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            try {
                accountManager.sessionKill(session.getAccount());

                return new ByteArray().put(SUCCESS);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = HEART_BEAT)
    public ByteArray heartBeat(ByteWrapper request, ServerSession session) {
        return new ByteArray().put(SUCCESS).put(request.getLong());
    }

    public void update(Long diff) {
        mapManager.update(diff);
        dataManager.update(diff);
        characterManager.update(diff);

        // for (ServerSession session: sessionList()) {
        //     session.write(new ByteArray().put((byte) 0x44));
        // }
    }
}
