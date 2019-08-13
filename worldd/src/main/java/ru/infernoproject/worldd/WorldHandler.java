package ru.infernoproject.worldd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanOperation;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.utils.ErrorUtils;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldMap;
import ru.infernoproject.worldd.script.ScriptManager;
import ru.infernoproject.worldd.script.ScriptValidationResult;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.map.WorldMapManager;
import ru.infernoproject.worldd.script.sql.Spell;
import ru.infernoproject.worldd.utils.MathUtils;
import ru.infernoproject.worldd.world.InternalCommand;
import ru.infernoproject.worldd.world.WorldTimer;
import ru.infernoproject.worldd.world.chat.ChatManager;
import ru.infernoproject.worldd.world.chat.ChatMessageType;
import ru.infernoproject.worldd.world.guild.GuildManager;
import ru.infernoproject.worldd.world.guild.sql.Guild;
import ru.infernoproject.worldd.world.guild.sql.GuildMember;
import ru.infernoproject.worldd.world.invite.InviteManager;
import ru.infernoproject.worldd.world.invite.InviteType;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import javax.script.ScriptException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static ru.infernoproject.common.constants.CommonErrorCodes.*;
import static ru.infernoproject.worldd.constants.WorldErrorCodes.*;
import static ru.infernoproject.worldd.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final WorldMapManager worldMapManager;
    private final ScriptManager scriptManager;
    private final ChatManager chatManager;
    private final GuildManager guildManager;
    private final InviteManager inviteManager;

    private final Map<String, Method> internalCommands;

    private final String serverName;

    public WorldHandler(DataSourceManager dataSourceManager, ConfigFile config) {
        super(dataSourceManager, config);

        worldMapManager = new WorldMapManager(dataSourceManager);
        scriptManager = new ScriptManager(dataSourceManager);
        chatManager = new ChatManager(worldMapManager);
        guildManager = new GuildManager(dataSourceManager);
        inviteManager = new InviteManager(worldMapManager, guildManager);

        internalCommands = registerInternalCommands();

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

    private Map<String, Method> registerInternalCommands() {
        logger.info("Looking for InternalCommands");
        Map<String, Method> internalCommands = new HashMap<>();

        for (Method method: getClass().getDeclaredMethods()) {
            if (!validateInternalCommand(method))
                continue;

            InternalCommand internalCommand = method.getAnnotation(InternalCommand.class);
            if (logger.isDebugEnabled())
                logger.debug(String.format("Command('%s'): %s", internalCommand.command(), method.getName()));
            internalCommands.put(internalCommand.command(), method);
        }
        logger.info("InternalCommands registered: {}", internalCommands.size());

        return internalCommands;
    }

    private boolean validateInternalCommand(Method action) {
        return action.isAnnotationPresent(InternalCommand.class) &&
            action.getReturnType().equals(ByteArray.class) &&
            action.getParameterCount() == 1 &&
            action.getParameterTypes()[0].equals(String[].class);
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

        return new ByteArray(SUCCESS)
            .put(session.characterInfo.location)
            .put(session.characterInfo)
            .put(player.getState())
            .put(WorldTimer.WORLD_TIMER.getServerTime())
            .put(WorldTimer.WORLD_TIMER.getServerTimeRate());
    }

    @ServerAction(opCode = EXECUTE, authRequired = true)
    public ByteArray executeCommand(ByteWrapper request, ServerSession session) throws Exception {
        Command command = scriptManager.getCommand(request.getString());

        if (command == null) {
            request.rewind();
            request.skip(1);

            return executeInternalCommand(request.getString(), request.getStrings(), session);
        }

        if (AccountLevel.hasAccess(session.getAccount().accessLevel, command.level)) {
            return new ByteArray(SUCCESS).put(
                command.execute(scriptManager, dataSourceManager, sessionManager.get(session.getAccount()), request.getStrings())
            );
        } else {
            return new ByteArray(AUTH_ERROR);
        }
    }

    private ByteArray executeInternalCommand(String command, String[] args, ServerSession session) throws Exception {
        logger.debug("Executing internal command '{}' with arguments: {}", command, args);

        if (internalCommands.containsKey(command)) {
            Method internalCommand = internalCommands.get(command);
            AccountLevel level = internalCommand.getAnnotation(InternalCommand.class).level();

            if (!AccountLevel.hasAccess(session.getAccount().accessLevel, level))
                return new ByteArray(AUTH_ERROR);

            internalCommand.setAccessible(true);

            try {
                return (ByteArray) internalCommand.invoke(this, (Object) args);
            } catch (IllegalAccessException e) {
                logger.error("Unable to execute internal command '{}': {}", command, e);

                return new ByteArray(SERVER_ERROR);
            } catch (InvocationTargetException e) {
                logger.error("Unable to execute internal command '{}': {}", command, e.getTargetException());

                return new ByteArray(SERVER_ERROR);
            }
        }

        return new ByteArray(INVALID_REQUEST);
    }

    @ServerAction(opCode = MOVE, authRequired = true)
    public ByteArray move(ByteWrapper request, ServerSession session) throws Exception {
        WorldPlayer player = ((WorldSession) session).getPlayer();
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

    @ServerAction(opCode = SPELL_LIST, authRequired = true)
    public ByteArray spellList(ByteWrapper request, ServerSession session) throws Exception {
        WorldPlayer player = ((WorldSession) session).getPlayer();
        List<Spell> spellList = dataSourceManager.query(Spell.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("required_class").eq(player.getCharacterInfo().classInfo.id),
                new SQLFilter("required_level").le(player.getCharacterInfo().level)
            )).fetchAll();

        return new ByteArray(SUCCESS).put(
            spellList.stream()
                .map(spell -> new ByteArray()
                    .put(spell.id).put(spell.name).put(spell.type.toString().toLowerCase())
                    .put(spell.distance).put(spell.radius).put(spell.basicPotential)
                    .put(spell.coolDown))
                .collect(Collectors.toList())
        );
    }

    @ServerAction(opCode = SPELL_CAST, authRequired = true)
    public ByteArray spellCast(ByteWrapper request, ServerSession session) throws Exception {
        WorldPlayer player = ((WorldSession) session).getPlayer();
        WorldMap map = worldMapManager.getMap(player.getPosition());

        Spell spell = dataSourceManager.query(Spell.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("id").eq(request.getInt()),
                new SQLFilter("required_class").eq(player.getCharacterInfo().classInfo.id),
                new SQLFilter("required_level").le(player.getCharacterInfo().level)
            )).fetchOne();

        if (spell != null) {
            if (player.hasCoolDown(spell.id)) {
                return new ByteArray(COOLDOWN).put(player.getCoolDown(spell.id));
            }

            switch (spell.type) {
                case SINGLE_TARGET:
                    return spellCastSingleTarget(map, spell, player, request);
                case AREA_OF_EFFECT:
                    return spellCastAreaOfEffect(map, spell, player, request);
            }
        }

        return new ByteArray(NOT_EXISTS);
    }

    private ByteArray spellCastSingleTarget(WorldMap map, Spell spell, WorldPlayer player, ByteWrapper target) throws ScriptException {
        WorldObject targetObject = map.findObjectById(target.getOID());

        if ((targetObject != null)&&(MathUtils.calculateDistance(player.getPosition(), targetObject.getPosition()) <= spell.distance)) {
            spell.cast(scriptManager, player, Collections.singletonList(targetObject));

            return new ByteArray(SUCCESS);
        }

        return new ByteArray(OUT_OF_RANGE);
    }

    private ByteArray spellCastAreaOfEffect(WorldMap map, Spell spell, WorldPlayer player, ByteWrapper target) throws ScriptException {
        WorldPosition targetPosition = new WorldPosition(
            map.getLocation().id,
            target.getFloat(),
            target.getFloat(),
            target.getFloat(),
            0f
        );

        if (MathUtils.calculateDistance(player.getPosition(), targetPosition) <= spell.distance) {
            List<WorldObject> targetList = map.findObjectsInArea(targetPosition, spell.radius);

            spell.cast(scriptManager, player, targetList);
            return new ByteArray(SUCCESS);
        }

        return new ByteArray(OUT_OF_RANGE);
    }

    @ServerAction(opCode = CHAT_MESSAGE, authRequired = true)
    public ByteArray chatMessageSend(ByteWrapper request, ServerSession session) throws Exception {
        ChatMessageType messageType = ChatMessageType.valueOf(request.getString().toUpperCase());

        String targetName = request.getString();
        String message = request.getString();

        WorldPlayer player = ((WorldSession) session).getPlayer();

        switch (messageType) {
            case LOCAL:
                chatManager.sendLocalMessage(player, message);

                return new ByteArray(SUCCESS).put(messageType).put(message);
            case BROADCAST:
                chatManager.sendBroadcastMessage(player, message);

                return new ByteArray(SUCCESS).put(messageType).put(message);
            case PRIVATE:
                WorldPlayer target = sessionList().stream()
                    .map(worldSession -> ((WorldSession) worldSession).getPlayer())
                    .filter(worldPlayer -> (worldPlayer != null) && worldPlayer.getName().equals(targetName))
                    .findFirst().orElse(null);

                if (target == null)
                    return new ByteArray(NOT_EXISTS);

                chatManager.sendPrivateMessage(player, target, message);

                return new ByteArray(SUCCESS).put(messageType).put(message);
            case PARTY:
                return new ByteArray(NOT_EXISTS);
            case GUILD:
                Guild guild = guildManager.getPlayerGuild(player.getCharacterInfo().id);

                if (Objects.isNull(guild)) {
                    return new ByteArray(NOT_EXISTS);
                }

                for (GuildMember guildMember: guildManager.getGuildPlayers(guild.id)) {
                    CharacterInfo guildPlayer = guildMember.character;

                    WorldPlayer targetMember = sessionList().stream()
                        .map(worldSession -> ((WorldSession) worldSession).getPlayer())
                        .filter(worldPlayer -> (worldPlayer != null) && worldPlayer.getName().equals(String.format("%s %s", guildPlayer.firstName, guildPlayer.lastName)))
                        .findFirst().orElse(null);

                    if (Objects.nonNull(targetMember)) {
                        chatManager.sendGuildMessage(player, targetMember, message);
                    }
                }

                return new ByteArray(SUCCESS).put(messageType).put(message);
            case ANNOUNCE:
                if (AccountLevel.isGameMaster(session.getAccount().accessLevel)) {
                    chatManager.sendAnnounce(message);
                    return new ByteArray(SUCCESS).put(messageType).put(message);
                } else {
                    return new ByteArray(AUTH_ERROR);
                }
        }

        return new ByteArray(INVALID_REQUEST);
    }

    @ServerAction(opCode = GUILD_CREATE, authRequired = true)
    public ByteArray guildCreate(ByteWrapper request, ServerSession session) throws Exception {
        CharacterInfo player = ((WorldSession) session).getPlayer().getCharacterInfo();
        Guild playerGuild = guildManager.getPlayerGuild(player.id);

        if (Objects.nonNull(playerGuild)) {
            return new ByteArray(COOLDOWN);
        }

        String title = request.getString();
        String tag = request.getString();
        String description = request.getString();

        playerGuild = guildManager.createGuild(title, tag, description, player);

        if (Objects.nonNull(playerGuild)) {
            return new ByteArray(SUCCESS).put(playerGuild.id);
        } else {
            return new ByteArray(INVALID_REQUEST);
        }
    }

    @ServerAction(opCode = GUILD_INVITE, authRequired = true)
    public ByteArray guildInvite(ByteWrapper request, ServerSession session) throws Exception {
        String targetName = request.getString();

        WorldPlayer player = ((WorldSession) session).getPlayer();
        Guild guild = guildManager.getPlayerGuild(player.getCharacterInfo().id);

        if (Objects.isNull(guild)) {
            return new ByteArray(INVALID_REQUEST);
        }

        WorldPlayer target = sessionList().stream()
            .map(worldSession -> ((WorldSession) worldSession).getPlayer())
            .filter(worldPlayer -> (worldPlayer != null) && worldPlayer.getName().equals(targetName))
            .findFirst().orElse(null);

        if (target == null)
            return new ByteArray(NOT_EXISTS);

        if (Objects.nonNull(guildManager.getPlayerGuild(target.getCharacterInfo().id))) {
            return new ByteArray(COOLDOWN);
        }

        inviteManager.sendInvite(InviteType.GUILD, player, target, new ByteArray().put(guild.id).put(guild.title));

        return new ByteArray(SUCCESS);
    }

    @ServerAction(opCode = GUILD_LEAVE, authRequired = true)
    public ByteArray guildLeave(ByteWrapper request, ServerSession session) throws Exception {
        WorldPlayer player = ((WorldSession) session).getPlayer();
        Guild guild = guildManager.getPlayerGuild(player.getCharacterInfo().id);

        if (Objects.isNull(guild)) {
            return new ByteArray(INVALID_REQUEST);
        }

        if (guildManager.getGuildMaster(guild.id).id == player.getCharacterInfo().id) {
            if (guildManager.getGuildPlayers(guild.id).size() == 1) {
                guildManager.removeGuildMember(player.getCharacterInfo());
                guildManager.removeGuild(guild.id);

                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(COOLDOWN);
            }
        } else {
            guildManager.removeGuildMember(player.getCharacterInfo());

            return new ByteArray(SUCCESS);
        }
    }

    @ServerAction(opCode = GUILD_PROMOTE, authRequired = true)
    public ByteArray guildPromote(ByteWrapper request, ServerSession session) throws Exception {
        WorldPlayer player = ((WorldSession) session).getPlayer();
        Guild guild = guildManager.getPlayerGuild(player.getCharacterInfo().id);

        if (Objects.isNull(guild)) {
            return new ByteArray(INVALID_REQUEST);
        }

        int playerLevel = guildManager.getPlayerLevel(guild, player.getCharacterInfo());

        int targetPlayer = request.getInt();
        int targetLevel = request.getInt();

        CharacterInfo targetPlayerCharacter = characterManager.get(targetPlayer);

        if (playerLevel == -1)
            return new ByteArray(AUTH_ERROR);

        if (targetPlayer == player.getCharacterInfo().id)
            return new ByteArray(INVALID_REQUEST);

        if ((targetLevel == 0) || (targetLevel < -1))
            return new ByteArray(INVALID_REQUEST);

        if ((playerLevel < targetLevel) || (targetLevel == -1)) {
            guildManager.setPlayerLevel(guild, targetPlayerCharacter, targetLevel);

            return new ByteArray(SUCCESS);
        } else if (playerLevel == 1) {
            guildManager.setPlayerLevel(guild, targetPlayerCharacter, 1);
            guildManager.setPlayerLevel(guild, player.getCharacterInfo(), 2);

            return new ByteArray(SUCCESS);
        } else {
            return new ByteArray(AUTH_ERROR);
        }
    }

    @ServerAction(opCode = GUILD_INFO, authRequired = true)
    public ByteArray guildInfo(ByteWrapper request, ServerSession session) throws Exception {
        int guildId = request.getInt();

        Guild guild;
        if (guildId == -1) {
            WorldPlayer player = ((WorldSession) session).getPlayer();
            guild = guildManager.getPlayerGuild(player.getCharacterInfo().id);
        } else {
            guild = guildManager.getGuild(guildId);
        }

        if (Objects.isNull(guild)) {
            return new ByteArray(NOT_EXISTS);
        }

        List<GuildMember> guildMembers = guildManager.getGuildPlayers(guild.id);

        return new ByteArray(SUCCESS)
            .put(guild.id)
            .put(guild.title)
            .put(guild.tag)
            .put(guildMembers);
    }

    @ServerAction(opCode = INVITE_RESPOND, authRequired = true)
    public ByteArray inviteRespond(ByteWrapper request, ServerSession session) throws Exception {
        long inviteId = request.getLong();
        boolean inviteAccepted = request.getBoolean();

        boolean result = inviteManager.respondToInvite(inviteId, inviteAccepted, ((WorldSession) session).getPlayer());

        return new ByteArray(result ? SUCCESS : NOT_EXISTS);
    }

    @ServerAction(opCode = SCRIPT_LIST, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptList(ByteWrapper request, ServerSession session) throws Exception {
        List<Script> scripts = scriptManager.listScripts();

        return new ByteArray(SUCCESS).put(
            scripts.stream()
                .map(script -> new ByteArray().put(script.id).put(script.name).put(script.language))
                .collect(Collectors.toList())
        );
    }

    @ServerAction(opCode = SCRIPT_LANGUAGE_LIST, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptLanguageList(ByteWrapper request, ServerSession session) throws Exception {
        List<String> languages = scriptManager.getAvailableLanguages();

        return new ByteArray(SUCCESS).put(
            languages.stream().map(language -> new ByteArray().put(language)).collect(Collectors.toList())
        );
    }

    @ServerAction(opCode = SCRIPT_GET, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptGet(ByteWrapper request, ServerSession session) throws Exception {
        Script script = scriptManager.getScript(request.getInt());

        if (script != null) {
            return new ByteArray(SUCCESS)
                .put(script.id)
                .put(script.name)
                .put(script.language)
                .put(script.script);
        } else {
            return new ByteArray(NOT_EXISTS);
        }
    }

    @ServerAction(opCode = SCRIPT_VALIDATE, authRequired = true, minLevel = AccountLevel.GAME_MASTER)
    public ByteArray scriptValidate(ByteWrapper request, ServerSession session) throws Exception {
        Script script = new Script();
        script.language = request.getString();
        script.script = request.getString();

        if (!scriptManager.getAvailableLanguages().contains(script.language))
            return new ByteArray(NOT_EXISTS);

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
        ScriptValidationResult result = scriptManager.updateScript(request.getInt(), request.getString(), request.getString());
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

    // Internal commands

    @InternalCommand(command = "setTime", level = AccountLevel.ADMIN)
    public ByteArray setServerTime(String[] args) {
        if (args.length < 1)
            return new ByteArray(SERVER_ERROR);

        if (!"-".equals(args[0])) {
            WorldTimer.WORLD_TIMER.setServerTime(Long.parseLong(args[0]) * 1000);
        }

        if (args.length >= 2) {
            WorldTimer.WORLD_TIMER.setServerTimeRate(Integer.parseInt(args[1]));
        }

        ByteArray timeChangeEvent = new ByteArray()
            .put(WorldTimer.WORLD_TIMER.getServerTime())
            .put(WorldTimer.WORLD_TIMER.getServerTimeRate());

        sessionList().parallelStream()
            .map(worldSession -> (WorldSession) worldSession)
            .forEach(
                worldSession -> worldSession.onEvent(
                    WorldEventType.TIME_CHANGE, new ByteArray()
                        .put(WorldObject.WORLD.getAttributes())
                        .put(timeChangeEvent)
                )
            );

        return new ByteArray(SUCCESS);
    }

    public void update(Long diff) {
        worldMapManager.update(diff);
    }

    @Override
    protected void onShutdown() {
        // Custom shutdown handling is not required
    }
}
