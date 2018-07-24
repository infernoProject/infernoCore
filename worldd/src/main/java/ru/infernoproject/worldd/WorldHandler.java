package ru.infernoproject.worldd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanOperation;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.utils.ErrorUtils;
import ru.infernoproject.worldd.map.WorldMap;
import ru.infernoproject.worldd.script.ScriptManager;
import ru.infernoproject.worldd.script.ScriptValidationResult;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.map.WorldMapManager;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import java.net.SocketAddress;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.infernoproject.common.constants.CommonErrorCodes.*;
import static ru.infernoproject.worldd.constants.WorldErrorCodes.*;
import static ru.infernoproject.worldd.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final WorldMapManager worldMapManager;
    private final ScriptManager scriptManager;

    private final String serverName;

    public WorldHandler(DataSourceManager dataSourceManager, ConfigFile config) {
        super(dataSourceManager, config);

        worldMapManager = new WorldMapManager(dataSourceManager);
        scriptManager = new ScriptManager(dataSourceManager);

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

        try {
            worldMapManager.readMapData(config.getFile("world.map.data_path", "maps"));
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
            ServerSession session = sessionGet(remoteAddress);
            WorldPlayer player = ((WorldSession) session).getPlayer();
            if (player != null) {
                CharacterInfo characterInfo = player.getCharacterInfo();
                WorldPosition position = player.getPosition();

                characterInfo.location = position.getLocation();

                characterInfo.positionX = position.getX();
                characterInfo.positionY = position.getY();
                characterInfo.positionZ = position.getZ();

                characterInfo.orientation = position.getOrientation();

                characterManager.update(characterInfo);

                player.destroy();
            }
            ((WorldSession) session).setPlayer(null);

            sessionManager.kill(session.getAccount());
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
        }
    }

    @ServerAction(opCode = AUTHORIZE)
    public ByteArray authorize(ByteWrapper request, ServerSession serverSession) throws Exception {
        Session session = sessionManager.get(request.getBytes());
        if (session == null)
            return new ByteArray(AUTH_ERROR);

        Account account = sessionManager.authorize(session, serverSession.address());
        if (account == null)
            return new ByteArray(AUTH_ERROR);

        if (session.characterInfo == null)
            return new ByteArray(AUTH_ERROR);

        if (session.characterInfo.realm.id != realmList.get(serverName).id)
            return new ByteArray(AUTH_ERROR);

        serverSession.setAuthorized(true);
        serverSession.setAccount(account);

        WorldPlayer player = new WorldPlayer((WorldSession) serverSession, session.characterInfo);

        player.updatePosition(player.getPosition(), worldMapManager.getMap(player.getPosition()));

        ((WorldSession) serverSession).setPlayer(player);

        return new ByteArray(SUCCESS).put(session.characterInfo.location).put(session.characterInfo);
    }

    @ServerAction(opCode = EXECUTE, authRequired = true)
    public ByteArray executeCommand(ByteWrapper request, ServerSession session) throws Exception {
        Command command = scriptManager.getCommand(request.getString());

        if (command == null)
            return new ByteArray(NOT_EXISTS);

        if (AccountLevel.hasAccess(session.getAccount().accessLevel, command.level)) {
            return new ByteArray(SUCCESS).put(
                command.execute(scriptManager, dataSourceManager, sessionManager.get(session.getAccount()), request.getStrings())
            );
        } else {
            return new ByteArray(AUTH_ERROR);
        }
    }

    @ServerAction(opCode = MOVE, authRequired = true)
    public ByteArray move(ByteWrapper request, ServerSession sesssion) throws Exception {
        WorldPlayer player = ((WorldSession) sesssion).getPlayer();
        WorldMap map = worldMapManager.getMap(player.getPosition());

        try {
            WorldPosition position = new WorldPosition(
                map.getLocation().id,
                request.getFloat(),
                request.getFloat(),
                request.getFloat(),
                request.getFloat()
            );

            if (map.isLegalMove(player.getPosition(), position)) {
                player.updatePosition(position, map);
                return new ByteArray(SUCCESS).put(position);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal move: {}", e.getMessage());
        }

        return new ByteArray(ILLEGAL_MOVE).put(player.getPosition());
    }

    @ServerAction(opCode = SCRIPT_LIST, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptList(ByteWrapper request, ServerSession session) throws Exception {
        List<Script> scripts = scriptManager.listScripts();

        return new ByteArray(SUCCESS).put(
            scripts.stream()
                .map(script -> new ByteArray().put(script.id).put(script.name))
                .collect(Collectors.toList())
        );
    }

    @ServerAction(opCode = SCRIPT_GET, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptGet(ByteWrapper request, ServerSession session) throws Exception {
        Script script = scriptManager.getScript(request.getInt());

        if (script != null) {
            return new ByteArray(SUCCESS)
                .put(script.id)
                .put(script.name)
                .put(script.script);
        } else {
            return new ByteArray(NOT_EXISTS);
        }
    }

    @ServerAction(opCode = SCRIPT_VALIDATE, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptValidate(ByteWrapper request, ServerSession session) throws Exception {
        Script script = new Script();
        script.script = request.getString();

        ScriptValidationResult result = scriptManager.validateScript(script);
        if (result.isValid()) {
            return new ByteArray(SUCCESS);
        } else {
            return new ByteArray(INVALID_SCRIPT)
                .put(result.getLine())
                .put(result.getColumn())
                .put(result.getMessage());
        }
    }

    @ServerAction(opCode = SCRIPT_SAVE, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptSave(ByteWrapper request, ServerSession session) throws Exception {
        ScriptValidationResult result = scriptManager.updateScript(request.getInt(), request.getString());
        if (result.isValid()) {
            return new ByteArray(SUCCESS);
        } else {
            return new ByteArray(INVALID_SCRIPT)
                .put(result.getLine())
                .put(result.getColumn())
                .put(result.getMessage());
        }
    }

    @ServerAction(opCode = LOG_OUT, authRequired = true)
    public ByteArray logOut(ByteWrapper request, ServerSession session) throws Exception {
        sessionManager.kill(session.getAccount());

        return new ByteArray(SUCCESS);
    }

    @ServerAction(opCode = HEART_BEAT)
    public ByteArray heartBeat(ByteWrapper request, ServerSession session) throws Exception {
        return new ByteArray(SUCCESS).put(request.getLong());
    }

    @InfernoMBeanOperation(description = "Get distribution of character by classes")
    public Map<String, Integer> getCharacterByClassDistribution() {
        try {
            return characterManager.getClassDistribution(realmList.get(serverName)).stream()
                .collect(Collectors.toMap(
                    distribution -> distribution.classInfo.name,
                    distribution -> distribution.count
                ));
        } catch (SQLException e) {
            ErrorUtils.logger(logger).error("Unable to calculate metric", e);
        }

        return new HashMap<>();
    }

    @InfernoMBeanOperation(description = "Get distribution of character by races")
    public Map<String, Integer> getCharacterByRaceDistribution() {
        try {
            return characterManager.getRaceDistribution(realmList.get(serverName)).stream()
                .collect(Collectors.toMap(
                    distribution -> distribution.raceInfo.name,
                    distribution -> distribution.count
                ));
        } catch (SQLException e) {
            ErrorUtils.logger(logger).error("Unable to calculate metric", e);
        }

        return new HashMap<>();
    }

    @InfernoMBeanOperation(description = "Get distribution of character by gender")
    public Map<String, Integer> getCharacterByGenderDistribution() {
        try {
            return characterManager.getGenderDistribution(realmList.get(serverName)).stream()
                .collect(Collectors.toMap(
                    distribution -> distribution.gender,
                    distribution -> distribution.count
                ));
        } catch (SQLException e) {
            ErrorUtils.logger(logger).error("Unable to calculate metric", e);
        }

        return new HashMap<>();
    }

    public void update(Long diff) {
        worldMapManager.update(diff);
    }

    @Override
    protected void onShutdown() {
        // Custom shutdown handling is not required
    }
}
