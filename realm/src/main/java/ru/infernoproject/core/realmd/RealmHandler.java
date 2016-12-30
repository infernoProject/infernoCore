package ru.infernoproject.core.realmd;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerSession;
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
import ru.infernoproject.core.common.net.SessionHelper;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteWrapper;
import ru.infernoproject.core.realmd.srp.SRP6Engine;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.RealmOperations.*;

@ChannelHandler.Sharable
public class RealmHandler extends ServerHandler {

    private final SRP6Engine srp6Engine;
    private final RealmList realmList;

    public RealmHandler(DataSourceManager dataSourceManager, SRP6Engine srp6Engine) {
        super(dataSourceManager);

        this.srp6Engine = srp6Engine;
        this.realmList = new RealmList(dataSourceManager);
    }

    @ServerAction(opCode = SRP6_CONFIG)
    public ByteArray getSRP6Config(ByteWrapper request, ServerSession session) {
        SRP6CryptoParams cryptoParams = srp6Engine.getCryptoParams();
        
        return new ByteArray()
            .put(cryptoParams.N)
            .put(cryptoParams.g)
            .put(cryptoParams.H);
    }

    @ServerAction(opCode = SIGN_UP)
    public ByteArray signUp(ByteWrapper request, ServerSession session) {
        String login = request.getString();
        BigInteger salt = request.getBigInteger();
        BigInteger verifier = request.getBigInteger();
        
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement existsQuery = connection.prepareStatement(
                "SELECT login FROM accounts WHERE login = ?"
            );

            existsQuery.setString(1, login);
            try (ResultSet resultSet = existsQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new ByteArray().put(AUTH_ERROR);
                } else {
                    PreparedStatement insertQuery = connection.prepareStatement(
                        "INSERT INTO accounts (login, level, salt, verifier) VALUES (?, ?, ?, ?)"
                    );

                    insertQuery.setString(1, login);
                    insertQuery.setInt(2, 1);
                    insertQuery.setString(3, HexBin.encode(salt.toByteArray()));
                    insertQuery.setString(4, HexBin.encode(verifier.toByteArray()));

                    insertQuery.execute();

                    return new ByteArray().put(SUCCESS);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return new ByteArray().put(SQL_ERROR);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP1)
    public ByteArray logInStep1(ByteWrapper request, ServerSession session) {
        if (!((RealmSession) session).getSRP6Session().getState().equals(SRP6ServerSession.State.INIT)) {
            ((RealmSession) session).setSRP6Session(srp6Engine.getSession());
        }

        if (((RealmSession) session).getSRP6Session().getState().equals(SRP6ServerSession.State.INIT)) {
            String login = request.getString();

            try (Connection connection = dataSourceManager.getConnection("realmd")) {
                PreparedStatement query = connection.prepareStatement(
                    "SELECT id, login, salt, verifier FROM accounts WHERE login = ?"
                );
                query.setString(1, login);

                try (ResultSet resultSet = query.executeQuery()) {
                    if (resultSet.next()) {
                        BigInteger salt = new BigInteger(HexBin.decode(resultSet.getString("salt")));
                        BigInteger verifier = new BigInteger(HexBin.decode(resultSet.getString("verifier")));

                        BigInteger B = ((RealmSession) session).getSRP6Session().step1(
                            resultSet.getString("login"),
                            salt, verifier
                        );

                        ((RealmSession) session).setAccountID(resultSet.getInt("id"));

                        return new ByteArray().put(SUCCESS).put(salt).put(B);
                    } else {
                        return new ByteArray().put(AUTH_ERROR);
                    }
                }
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_INVALID);
        }
    }

    @ServerAction(opCode = LOG_IN_STEP2)
    public ByteArray logInStep2(ByteWrapper request, ServerSession session) {
        if (((RealmSession) session).getSRP6Session().getState().equals(SRP6ServerSession.State.STEP_1)) {
            BigInteger A = request.getBigInteger();
            
            BigInteger M1 = request.getBigInteger();

            try {
                BigInteger M2 = ((RealmSession) session).getSRP6Session().step2(A, M1);

                if (((RealmSession) session).getSRP6Session().getState().equals(SRP6ServerSession.State.STEP_2)) {
                    ((RealmSession) session).setAuthorized(true);
                }

                return new ByteArray().put(SUCCESS).put(M2);
            } catch (SRP6Exception e) {
                logger.error("SRP6Error: {} : {}", e.getMessage(), e.getCauseType());
                return new ByteArray().put(AUTH_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_INVALID);
        }
    }

    @ServerAction(opCode = SESSION_TOKEN)
    public ByteArray getSessionToken(ByteWrapper request, ServerSession session) {
        if (((RealmSession) session).isAuthorized()) {
            try (Connection connection = dataSourceManager.getConnection("realmd")) {
                PreparedStatement sessionKiller = connection.prepareStatement(
                    "DELETE FROM sessions WHERE account = ?"
                );

                sessionKiller.setInt(1, ((RealmSession) session).getAccountID());
                sessionKiller.execute();

                PreparedStatement sessionCreator = connection.prepareStatement(
                    "INSERT INTO sessions (account, session_key) VALUES (?, ?)"
                );

                byte[] sessionKey = SessionHelper.generateSessionKey();

                sessionCreator.setInt(1, ((RealmSession) session).getAccountID());
                sessionCreator.setString(2, HexBin.encode(sessionKey));

                sessionCreator.execute();

                return new ByteArray().put(SUCCESS).put(sessionKey);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @ServerAction(opCode = REALM_LIST)
    public ByteArray getRealmList(ByteWrapper request, ServerSession session) {
        if (((RealmSession) session).isAuthorized()) {
            try {
                List<RealmServerInfo> realmServerList = realmList.listRealmServers();

                return new ByteArray().put(SUCCESS).put(realmServerList);
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                return new ByteArray().put(SQL_ERROR);
            }
        } else {
            return new ByteArray().put(AUTH_REQUIRED);
        }
    }

    @Override
    protected ServerSession onSessionInit(SocketAddress remoteAddress) {
        RealmSession realmSession = new RealmSession();

        realmSession.setSRP6Session(srp6Engine.getSession());

        return realmSession;
    }

    @Override
    protected void onSessionClose(SocketAddress remoteAddress) {

    }
}
