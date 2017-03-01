package ru.infernoproject.worldd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import ru.infernoproject.common.auth.AccountManager;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.impl.Account;
import ru.infernoproject.common.auth.impl.Session;
import ru.infernoproject.common.utils.LevelCompare;
import ru.infernoproject.worldd.data.MovementInfo;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.characters.sql.CharacterInfo;
import ru.infernoproject.worldd.characters.CharacterManager;
import ru.infernoproject.worldd.data.sql.ClassInfo;
import ru.infernoproject.worldd.data.sql.RaceInfo;
import ru.infernoproject.worldd.data.WorldDataManager;
import ru.infernoproject.worldd.map.MapManager;
import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.impl.Command;
import ru.infernoproject.worldd.scripts.impl.Spell;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import javax.script.ScriptException;
import java.net.SocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static ru.infernoproject.common.constants.ErrorCodes.*;
import static ru.infernoproject.common.constants.WorldOperations.*;

@ChannelHandler.Sharable
public class WorldHandler extends ServerHandler {

    private final CharacterManager characterManager;
    private final WorldDataManager dataManager;
    private final ScriptManager scriptManager;

    private final MapManager mapManager;

    public WorldHandler(DataSourceManager dataSourceManager, AccountManager accountManager) {
        super(dataSourceManager, accountManager);

        dataManager = new WorldDataManager(dataSourceManager);
        scriptManager = new ScriptManager(dataSourceManager);
        characterManager = new CharacterManager(dataSourceManager, scriptManager);

        mapManager = new MapManager();
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
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = EXECUTE)
    public ByteArray commandExecute(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray().put(AUTH_REQUIRED);

        String command = request.getString();
        String[] arguments = request.getStrings();

        try {
            Command commandInstance = scriptManager.commandGet(command)
                .getCommand(scriptManager);

            if (commandInstance == null)
                return new ByteArray().put(UNKNOWN_COMMAND);

            commandInstance.setDataSourceManager(dataSourceManager);
            commandInstance.setCharacterManager(characterManager);

            commandInstance.setSession(session);
            commandInstance.setSessions(sessionList());

            if (LevelCompare.toInteger(commandInstance.getLevel()) <= LevelCompare.toInteger(session.getAccount().getAccessLevel())) {
                PyTuple result = commandInstance.execute(arguments);

                Integer exitCode = (Integer) result.get(0);

                List<String> output = new ArrayList<>();
                for (PyObject pyObject: ((PyList) result.get(1)).getArray()) {
                    output.add(pyObject.asString());
                }

                return new ByteArray().put(SUCCESS).put(exitCode).put(output.toArray(new String[] {}));
            } else {
                return new ByteArray().put(AUTH_REQUIRED);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        } catch (PyException e) {
            logger.error("PyError({}): {}", e.type.getClass().getSimpleName(), e.value);
            return new ByteArray().put(SERVER_ERROR);
        } catch (ScriptException e) {
            logger.error("ScriptError({}:{}): {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_LIST)
    public ByteArray characterListGet(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray().put(AUTH_REQUIRED);

        try {
            List<CharacterInfo> characterInfoList = characterManager.characterList((WorldSession) session);

            return new ByteArray().put(SUCCESS).put(characterInfoList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_CREATE)
    public ByteArray characterCreate(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray().put(AUTH_REQUIRED);

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
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_SELECT)
    public ByteArray characterSelect(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray().put(AUTH_REQUIRED);

        int characterId = request.getInt();

        try {
            CharacterInfo characterInfo = characterManager.characterGet(characterId, (WorldSession) session);

            if (characterInfo != null) {
                ((WorldSession) session).setPlayer(new WorldPlayer(
                    ((WorldSession) session), characterInfo
                ));

                return new ByteArray().put(SUCCESS).put(characterInfo);
            }

            return new ByteArray().put(CHARACTER_NOT_EXISTS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = RACE_LIST)
    public ByteArray raceListGet(ByteWrapper request, ServerSession session) {
        try {
            List<RaceInfo> raceList = dataManager.raceList();

            return new ByteArray().put(SUCCESS).put(raceList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CLASS_LIST)
    public ByteArray classListGet(ByteWrapper request, ServerSession session) {
        try {
            List<ClassInfo> classList = dataManager.classList();

            return new ByteArray().put(SUCCESS).put(classList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
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
            return new ByteArray().put(INVALID_REQUEST);
        }

        if (opCode == MOVE_FALL_LAND) {
            ((WorldSession) session).getPlayer().handleFall(move);
        }

        ((WorldSession) session).getPlayer().handleMove(opCode, move);

        return new ByteArray().put(SUCCESS);
    }

    @ServerAction(opCode = SPELL_CAST)
    public ByteArray spellCast(ByteWrapper request, ServerSession session) {
        int spellId = request.getInt();

        try {
            Spell spell = scriptManager.spellGet(spellId)
                .getSpell(scriptManager);

            WorldPlayer player = ((WorldSession) session).getPlayer();

            if (player == null)
                return new ByteArray().put(NOT_IN_GAME);

            if (!characterManager.spellLearned(player.getCharacterInfo(), spellId))
                return new ByteArray().put(SPELL_NOT_LEARNED);

            if (player.isDead())
                return new ByteArray().put(PLAYER_DEAD);

            if (player.hasCoolDown(spell.getId()))
                return new ByteArray().put(SPELL_COOL_DOWN);

            spell.cast(player, new WorldCreature[]{ player });
            player.addCoolDown(spell.getId(), spell.getCoolDown());

            return new ByteArray().put(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        } catch (ScriptException e) {
            logger.error("ScriptError({}:{}): {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = LOG_OUT)
    public ByteArray logOut(ByteWrapper request, ServerSession session) {
        if (!session.isAuthorized())
            return new ByteArray().put(AUTH_REQUIRED);

        try {
            accountManager.sessionKill(session.getAccount());

            return new ByteArray().put(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
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

        sessionList().parallelStream().forEach(session -> {
            WorldPlayer player = ((WorldSession) session).getPlayer();
            if (player != null) {
                player.update(diff);
            }
        });
    }
}
