package ru.infernoproject.core.realmd;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.auth.AccountManager;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.net.server.ServerAction;
import ru.infernoproject.core.common.net.server.ServerHandler;
import ru.infernoproject.core.common.net.server.ServerSession;
import ru.infernoproject.core.common.types.auth.Session;
import ru.infernoproject.core.common.types.auth.LogInStep1Challenge;
import ru.infernoproject.core.common.types.auth.LogInStep2Challenge;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteWrapper;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.List;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.RealmOperations.*;

@ChannelHandler.Sharable
public class RealmHandler extends ServerHandler {

    private final RealmList realmList;

    public RealmHandler(DataSourceManager dataSourceManager, AccountManager accountManager) {
        super(dataSourceManager, accountManager);

        this.realmList = new RealmList(dataSourceManager);
    }

    @ServerAction(opCode = SRP6_CONFIG)
    public ByteArray getSRP6Config(ByteWrapper request, ServerSession session) {
        SRP6CryptoParams cryptoParams = accountManager.cryptoParamsGet();
        
        return new ByteArray()
            .put(cryptoParams.N)
            .put(cryptoParams.g)
            .put(cryptoParams.H);
    }

    @ServerAction(opCode = SIGN_UP)
    public ByteArray signUp(ByteWrapper request, ServerSession session) {
        String login = request.getString();
        String email = request.getString();
        BigInteger salt = request.getBigInteger();
        BigInteger verifier = request.getBigInteger();
        
        try {
            Account account = accountManager.accountCreate(login, email, salt, verifier);

            if (account != null) {
                session.setAccount(account);

                return new ByteArray().put(SUCCESS);
            } else {
                return new ByteArray().put(ALREADY_EXISTS);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP1)
    public ByteArray logInStep1(ByteWrapper request, ServerSession serverSession) {
        String login = request.getString();

        try {
            LogInStep1Challenge challenge = accountManager.accountLogInStep1(serverSession.address(), login);

            if (challenge.isSuccess()) {
                return new ByteArray().put(SUCCESS)
                    .put(challenge.getSession().getKey())
                    .put(challenge.getSalt())
                    .put(challenge.getB());
            } else {
                return new ByteArray().put(AUTH_ERROR);
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP2)
    public ByteArray logInStep2(ByteWrapper request, ServerSession serverSession) {
        try {
            Session session = accountManager.sessionGet(
                request.getBytes()
            );

            BigInteger A = request.getBigInteger();
            BigInteger M1 = request.getBigInteger();

            LogInStep2Challenge challenge = accountManager.accountLogInStep2(session, A, M1);

            if (challenge.isSuccess()) {
                serverSession.setAuthorized(true);
                serverSession.setAccount(session.getAccount());

                return new ByteArray().put(SUCCESS).put(challenge.getM2());
            } else {
                return new ByteArray().put(AUTH_INVALID);
            }
        } catch (SRP6Exception e) {
            logger.error("SRP6Error: {} : {}", e.getMessage(), e.getCauseType());
            return new ByteArray().put(AUTH_ERROR);
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SERVER_ERROR);
        }
    }

    @ServerAction(opCode = SESSION_TOKEN)
    public ByteArray getSessionToken(ByteWrapper request, ServerSession serverSession) {
        if (serverSession.isAuthorized()) {
            try {
                Session session = accountManager.sessionGet(serverSession.getAccount());

                return new ByteArray().put(SUCCESS).put(session.getKey());
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SERVER_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = REALM_LIST)
    public ByteArray getRealmList(ByteWrapper request, ServerSession session) {
        if (session.isAuthorized()) {
            try {
                List<RealmServerInfo> realmServerList = realmList.listRealmServers();

                return new ByteArray().put(SUCCESS).put(realmServerList);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SERVER_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @Override
    protected ServerSession onSessionInit(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        return new RealmSession(ctx, remoteAddress);
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {

    }
}
