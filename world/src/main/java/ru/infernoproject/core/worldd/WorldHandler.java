package ru.infernoproject.core.worldd;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.core.common.db.DataSourceManager;
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
public class WorldHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WorldHandler.class);

    private final DataSourceManager dataSourceManager;
    private final Map<SocketAddress, WorldSession> sessions = new ConcurrentHashMap<>();

    private final WorldCommander commander;

    private final CharacterManager characterManager;
    private final WorldDataManager dataManager;

    public WorldHandler(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

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
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        WorldSession session = new WorldSession(this, dataSourceManager);

        sessions.put(remoteAddress, session);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();

        sessions.remove(remoteAddress);
        super.channelUnregistered(ctx);
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteWrapper request = (ByteWrapper) msg;
        ByteArray response;

        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        WorldSession session = sessions.get(remoteAddress);

        byte opCode = request.getByte();
        switch (opCode) {
            case AUTHORIZE:
                response = authorize(request, session);
                break;
            case EXECUTE:
                response = commandExecute(request, session);
                break;
            case CHARACTER_LIST:
                response = characterListGet(request, session);
                break;
            case CHARACTER_CREATE:
                response = characterCreate(request, session);
                break;
            case CHARACTER_SELECT:
                response = characterSelect(request, session);
                break;
            case RACE_LIST:
                response = raceListGet(request, session);
                break;
            case CLASS_LIST:
                response = classListGet(request, session);
                break;
            case LOG_OUT:
                response = logOut(request, session);
                break;
            default:
                response = new ByteArray().put(UNKNOWN_OPCODE);
                break;
        }

        session.update();

        ctx.write(new ByteArray().put(opCode).put(response).toByteArray());
    }

    private ByteArray authorize(ByteWrapper request, WorldSession session) {
        byte[] sessionKey = request.getBytes();

        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT sessions.account, accounts.level+0 as level FROM sessions " +
                "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
            );

            sessionQuery.setString(1, HexBin.encode(sessionKey));

            try (ResultSet resultSet = sessionQuery.executeQuery())  {
                if (resultSet.next()) {
                    session.setAuthorized(true);
                    session.setSessionKey(sessionKey);
                    session.setAccountID(resultSet.getInt("account"));
                    session.setAccessLevel(resultSet.getInt("level"));

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

    private ByteArray commandExecute(ByteWrapper request, WorldSession session) {
        String command = request.getString();
        String[] arguments = request.getStrings();

        WorldCommand commandInstance = commander.getCommand(command, session.getAccessLevel());
        if (commandInstance != null) {
            WorldCommandResult result = commandInstance.execute(dataSourceManager, session, arguments);

            return new ByteArray().put(SUCCESS).put(result);
        } else {
            return new ByteArray().put(UNKNOWN_COMMAND);
        }
    }

    private ByteArray characterListGet(ByteWrapper request, WorldSession session) {
        try {
            List<CharacterInfo> characterInfoList = characterManager.characterList(session);

            return new ByteArray().put(SUCCESS).put(characterInfoList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    private ByteArray characterCreate(ByteWrapper request, WorldSession session) {
        try {
            CharacterInfo characterInfo = characterManager.characterCreate(
                new CharacterInfo(request.getWrapper()), session
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

    private ByteArray characterSelect(ByteWrapper request, WorldSession session) {
        int characterId = request.getInt();

        try {
            CharacterInfo characterInfo = characterManager.characterGet(characterId, session);

            if (characterInfo != null) {
                return new ByteArray().put(SUCCESS).put(characterInfo);
            }

            return new ByteArray().put(CHARACTER_NOT_EXISTS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    private ByteArray raceListGet(ByteWrapper request, WorldSession session) {
        try {
            List<RaceInfo> raceList = dataManager.raceList();

            return new ByteArray().put(SUCCESS).put(raceList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    private ByteArray classListGet(ByteWrapper request, WorldSession session) {
        try {
            List<ClassInfo> classList = dataManager.classList();

            return new ByteArray().put(SUCCESS).put(classList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    private ByteArray logOut(ByteWrapper request, WorldSession session) {
        try {
            session.close();

            return new ByteArray().put(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    public WorldCommander getCommander() {
        return commander;
    }
}
