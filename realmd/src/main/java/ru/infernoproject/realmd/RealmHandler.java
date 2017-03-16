package ru.infernoproject.realmd;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.AccountManager;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.server.ServerAction;
import ru.infernoproject.common.server.ServerHandler;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;

import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import static ru.infernoproject.common.constants.ErrorCodes.*;
import static ru.infernoproject.common.constants.RealmOperations.*;

@ChannelHandler.Sharable
public class RealmHandler extends ServerHandler {

    private final RealmList realmList;

    public RealmHandler(DataSourceManager dataSourceManager, AccountManager accountManager) {
        super(dataSourceManager, accountManager);

        this.realmList = new RealmList(dataSourceManager);
    }

    @ServerAction(opCode = CRYPTO_CONFIG)
    public ByteArray getCryptoConfig(ByteWrapper request, ServerSession session) {
        return new ByteArray(SUCCESS).put(accountManager.getServerSalt());
    }

    @ServerAction(opCode = SIGN_UP)
    public ByteArray signUp(ByteWrapper request, ServerSession session) {
        String login = request.getString();
        String email = request.getString();

        byte[] salt = request.getBytes();
        byte[] verifier = request.getBytes();
        
        try {
            Account account = accountManager.accountCreate(login, email, salt, verifier);

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
            Session session = accountManager.accountLogInStep1(serverSession.address(), login);

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
            Session session = accountManager.sessionGet(
                request.getBytes()
            );

            if (accountManager.accountLogInStep2(session, request.getBytes())) {
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

    @ServerAction(opCode = SESSION_TOKEN)
    public ByteArray getSessionToken(ByteWrapper request, ServerSession serverSession) {
        if (serverSession.isAuthorized()) {
            try {
                Session session = accountManager.sessionGet(serverSession.getAccount());

                return new ByteArray(SUCCESS).put(session.getKey());
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray(SERVER_ERROR);
            }
        } else {
            return new ByteArray(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = REALM_LIST)
    public ByteArray getRealmList(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            try {
                List<RealmServerInfo> realmServerList = realmList.listRealmServers();

                return new ByteArray(SUCCESS).put(realmServerList);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray(SERVER_ERROR);
            }
        } else {
            return new ByteArray(AUTH_REQUIRED);
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
