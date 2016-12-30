package ru.infernoproject.core.worldd;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.net.ServerAction;
import ru.infernoproject.core.common.net.ServerHandler;
import ru.infernoproject.core.common.net.ServerSession;
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

import java.net.SocketAddress;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final WorldCommander commander;

    private final CharacterManager characterManager;
    private final WorldDataManager dataManager;

    public WorldHandler(DataSourceManager dataSourceManager) {
        super(dataSourceManager);

        commander = new WorldCommander();

        characterManager = new CharacterManager(dataSourceManager);
        dataManager = new WorldDataManager(dataSourceManager);

        registerCommands();
    }

    private void registerCommands() {
        commander.register(HelpCommand.class);
        commander.register(AccountCommand.class);
    }

    @Override
    protected ServerSession onSessionInit(SocketAddress remoteAddress) {
        return new WorldSession(dataSourceManager);
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {

    }

    @ServerAction(opCode = AUTHORIZE)
    public ByteArray authorize(ByteWrapper request, ServerSession session) {
        byte[] sessionKey = request.getBytes();

        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT sessions.account, accounts.level+0 as level FROM sessions " +
                "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
            );

            sessionQuery.setString(1, HexBin.encode(sessionKey));

            try (ResultSet resultSet = sessionQuery.executeQuery())  {
                if (resultSet.next()) {
                    ((WorldSession) session).setAuthorized(true);
                    ((WorldSession) session).setSessionKey(sessionKey);
                    ((WorldSession) session).setAccountID(resultSet.getInt("account"));
                    ((WorldSession) session).setAccessLevel(resultSet.getInt("level"));

                    return new ByteArray().put(SUCCESS);
                } else {
                    return new ByteArray().put(AUTH_ERROR);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = EXECUTE)
    public ByteArray commandExecute(ByteWrapper request, ServerSession session) {
        String command = request.getString();
        String[] arguments = request.getStrings();

        WorldCommand commandInstance = commander.getCommand(command, ((WorldSession) session).getAccessLevel());
        if (commandInstance != null) {
            WorldCommandResult result = commandInstance.execute(dataSourceManager, (WorldSession) session, arguments);

            return new ByteArray().put(SUCCESS).put(result);
        } else {
            return new ByteArray().put(UNKNOWN_COMMAND);
        }
    }

    @ServerAction(opCode = CHARACTER_LIST)
    public ByteArray characterListGet(ByteWrapper request, ServerSession session) {
        try {
            List<CharacterInfo> characterInfoList = characterManager.characterList((WorldSession) session);

            return new ByteArray().put(SUCCESS).put(characterInfoList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_CREATE)
    public ByteArray characterCreate(ByteWrapper request, ServerSession session) {
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
    }

    @ServerAction(opCode = CHARACTER_SELECT)
    public ByteArray characterSelect(ByteWrapper request, ServerSession session) {
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
        try {
            session.close();

            return new ByteArray().put(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }
}
