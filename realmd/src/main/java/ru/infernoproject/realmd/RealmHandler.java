package ru.infernoproject.realmd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.AccountBan;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.common.realmlist.RealmListEntry;

import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import static ru.infernoproject.common.constants.CommonErrorCodes.*;
import static ru.infernoproject.realmd.constants.RealmErrorCodes.*;
import static ru.infernoproject.realmd.constants.RealmOperations.*;

@ChannelHandler.Sharable
public class RealmHandler extends ServerHandler {

    public RealmHandler(DataSourceManager dataSourceManager, ConfigFile configFile) {
        super(dataSourceManager, configFile);
    }

    @ServerAction(opCode = CRYPTO_CONFIG)
    public ByteArray cryptoConfigGet(ByteWrapper request, ServerSession session) {
        return new ByteArray(SUCCESS).put(accountManager.serverSalt());
    }

    @ServerAction(opCode = SIGN_UP)
    public ByteArray signUp(ByteWrapper request, ServerSession session) {
        String login = request.getString();
        String email = request.getString();

        byte[] salt = request.getBytes();
        byte[] verifier = request.getBytes();
        
        try {
            Account account = accountManager.create(login, email, salt, verifier);

            if (account != null) {
                session.setAccount(account);

                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(ALREADY_EXISTS);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP1)
    public ByteArray logInStep1(ByteWrapper request, ServerSession serverSession) {
        String login = request.getString();

        try {
            Session session = accountManager.logInStep1(serverSession.address(), login);

            if (session != null) {
                return new ByteArray(SUCCESS)
                    .put(session.getKey())
                    .put(session.getAccount().getSalt())
                    .put(session.getVector());
            } else {
                return new ByteArray(AUTH_ERROR);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP2)
    public ByteArray logInStep2(ByteWrapper request, ServerSession serverSession) {
        try {
            Session session = sessionManager.get(
                request.getBytes()
            );

            AccountBan ban = accountManager.checkBan(session.account);
            if (ban != null) {
                return new ByteArray(USER_BANNED)
                    .put(ban.reason)
                    .put(ban.expires);
            }

            if (accountManager.logInStep2(session, request.getBytes())) {
                serverSession.setAuthorized(true);
                serverSession.setAccount(session.getAccount());

                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(AUTH_INVALID);
            }
        } catch (NoSuchAlgorithmException e) {
            return new ByteArray(AUTH_ERROR);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = SESSION_TOKEN, authRequired = true)
    public ByteArray sessionTokenGet(ByteWrapper request, ServerSession serverSession) {
        try {
            Session session = sessionManager.get(serverSession.getAccount());

            return new ByteArray(SUCCESS).put(session.getKey());
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = REALM_LIST, authRequired = true)
    public ByteArray realmListGet(ByteWrapper request, ServerSession session) {
        try {
            List<RealmListEntry> realmServerList = realmList.list();

            return new ByteArray(SUCCESS).put(realmServerList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = RACE_LIST, authRequired = true)
    public ByteArray raceListGet(ByteWrapper request, ServerSession session) {
        try {
            List<RaceInfo> raceList = dataManager.raceList();

            return new ByteArray(SUCCESS).put(raceList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CLASS_LIST, authRequired = true)
    public ByteArray classListGet(ByteWrapper request, ServerSession session) {
        try {
            List<ClassInfo> classList = dataManager.classList();

            return new ByteArray(SUCCESS).put(classList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_LIST, authRequired = true)
    public ByteArray characterListGet(ByteWrapper request, ServerSession session) {
        try {
            List<CharacterInfo> characterList = characterManager.list(session.getAccount());

            return new ByteArray(SUCCESS).put(characterList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_CREATE, authRequired = true)
    public ByteArray characterCreate(ByteWrapper request, ServerSession session) {
        try {
            CharacterInfo characterInfo = new CharacterInfo();
            characterInfo.realm = realmList.get(request.getInt());
            characterInfo.account = session.getAccount();

            characterInfo.firstName = request.getString();
            characterInfo.lastName = request.getString();

            characterInfo.gender = Enum.valueOf(GenderInfo.class, request.getString().toUpperCase());

            characterInfo.raceInfo = dataManager.raceGetById(request.getInt());
            characterInfo.classInfo = dataManager.classGetById(request.getInt());

            characterInfo.body = request.getBytes();

            int characterId = characterManager.create(characterInfo);
            if (characterId > 0) {
                return new ByteArray(SUCCESS).put(characterId);
            } else {
                return new ByteArray(CHARACTER_EXISTS);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_SELECT, authRequired = true)
    public ByteArray characterSelect(ByteWrapper request, ServerSession session) {
        try {
            CharacterInfo characterInfo = characterManager.get(request.getInt());

            if ((characterInfo == null) || (characterInfo.account.id != session.getAccount().id))
                return new ByteArray(CHARACTER_NOT_FOUND);

            Session playerSession = sessionManager.get(session.getAccount());

            playerSession.characterInfo = characterInfo;

            return new ByteArray(SUCCESS);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_DELETE, authRequired = true)
    public ByteArray characterDelete(ByteWrapper request, ServerSession session) {
        try {
            CharacterInfo characterInfo = characterManager.get(request.getInt());

            if ((characterInfo == null) || (characterInfo.account.id != session.getAccount().id))
                return new ByteArray(CHARACTER_NOT_FOUND);

            if (characterManager.delete(characterInfo)) {
                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(CHARACTER_DELETED);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_RESTOREABLE_LIST, authRequired = true)
    public ByteArray characterGetRestoreableList(ByteWrapper request, ServerSession session) {
        try {
            List<CharacterInfo> characterList = characterManager.list_deleted(session.getAccount());

            return new ByteArray(SUCCESS).put(characterList);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = CHARACTER_RESTORE, authRequired = true)
    public ByteArray characterRestore(ByteWrapper request, ServerSession session) {
        try {
            CharacterInfo characterInfo = characterManager.get(request.getInt());

            if ((characterInfo == null) || (characterInfo.account.id != session.getAccount().id))
                return new ByteArray(CHARACTER_NOT_FOUND);

            if (characterManager.restore(characterInfo)) {
                return new ByteArray(SUCCESS);
            } else {
                return new ByteArray(CHARACTER_EXISTS);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray(SERVER_ERROR);
        }
    }

    @Override
    protected ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        return new RealmSession(ctx, remoteAddress);
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {
        // Custom session termination is not required
    }
}
