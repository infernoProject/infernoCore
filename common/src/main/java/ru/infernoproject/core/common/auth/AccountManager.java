package ru.infernoproject.core.common.auth;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerSession;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.config.ConfigFile;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.error.CoreException;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.types.auth.Session;
import ru.infernoproject.core.common.srp.SRP6Engine;
import ru.infernoproject.core.common.types.auth.LogInStep1Challenge;
import ru.infernoproject.core.common.types.auth.LogInStep2Challenge;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {

    private final DataSourceManager dataSourceManager;
    private final ConfigFile config;

    private final SRP6Engine srp6Engine;
    private final Map<String, SRP6ServerSession> cryptoSessions;

    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(AccountManager.class);

    public AccountManager(DataSourceManager dataSourceManager, ConfigFile config) {
        this.dataSourceManager = dataSourceManager;
        this.config = config;

        this.srp6Engine = new SRP6Engine(config);
        this.cryptoSessions = new ConcurrentHashMap<>();
    }

    public Account accountCreate(String login, String email, BigInteger salt, BigInteger verifier) throws CoreException {
        Account account = accountGet(login);

        if (account == null) {
            dataSourceManager.query(
                "realmd", "INSERT INTO accounts (login, level, email, salt, verifier) VALUES (?, ?, ?, ?, ?)"
            ).configure((query) -> {
                query.setString(1, login);
                query.setInt(2, 1);
                query.setString(3, email);
                query.setString(4, HexBin.encode(salt.toByteArray()));
                query.setString(5, HexBin.encode(verifier.toByteArray()));
            }).executeUpdate();

            return accountGet(login);
        }

        return null;
    }

    public Account accountGet(String login) throws CoreException {
        return (Account) dataSourceManager.query(
            "realmd", "SELECT id, level+0 as level, login FROM accounts WHERE login = ?"
        ).configure((query) -> {
            query.setString(1, login);
        }).executeSelect((result) -> {
            if (result.next()) {
                return new Account(
                    result.getInt("id"),
                    result.getInt("level"),
                    result.getString("login")
                );
            }

            return null;
        });
    }

    public LogInStep1Challenge accountLogInStep1(SocketAddress remoteAddress, String login) throws CoreException {
        return (LogInStep1Challenge) dataSourceManager.query(
            "realmd", "SELECT id, login, salt, verifier, level+0 as level FROM accounts WHERE login = ?"
        ).configure((query) -> {
            query.setString(1, login);
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                BigInteger salt = new BigInteger(HexBin.decode(resultSet.getString("salt")));
                BigInteger verifier = new BigInteger(HexBin.decode(resultSet.getString("verifier")));

                SRP6ServerSession cryptoSession = srp6Engine.getSession();

                BigInteger b = cryptoSession.step1(
                    resultSet.getString("login"),
                    salt, verifier
                );

                Account account = new Account(
                    resultSet.getInt("id"),
                    resultSet.getInt("level"),
                    resultSet.getString("login")
                );

                Session session = sessionCreate(account, remoteAddress);

                cryptoSessions.put(session.getKeyHex(), cryptoSession);

                return new LogInStep1Challenge(session, salt, b);
            }

            return new LogInStep1Challenge();
        });
    }

    public LogInStep2Challenge accountLogInStep2(Session session, BigInteger a, BigInteger m1) throws SRP6Exception {
        SRP6ServerSession cryptoSession = cryptoSessions.get(session.getKeyHex());

        if (cryptoSession.getState().equals(SRP6ServerSession.State.STEP_1)) {
            BigInteger m2 = cryptoSession.step2(a, m1);

            return new LogInStep2Challenge(m2);
        }

        return new LogInStep2Challenge();
    }

    public SRP6CryptoParams cryptoParamsGet() {
        return srp6Engine.getCryptoParams();
    }

    public Account sessionAuthorize(Session session, SocketAddress sessionAddress) throws CoreException {
        return (Account) dataSourceManager.query(
            "realmd", "SELECT sessions.account, accounts.level+0 as level, accounts.login FROM sessions " +
                "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
        ).configure((query) -> {
            query.setString(1, HexBin.encode(session.getKey()));
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                dataSourceManager.query(
                    "realmd","UPDATE sessions SET session_address = ? WHERE session_key = ?"
                ).configure((query) -> {
                    query.setString(1, sessionAddress.toString());
                    query.setString(2, HexBin.encode(session.getKey()));
                }).executeUpdate();

                return new Account(
                    resultSet.getInt("account"),
                    resultSet.getInt("level"),
                    resultSet.getString("login")
                );
            }

            return null;
        });
    }

    public Session sessionCreate(Account account, SocketAddress remoteAddress) throws CoreException {
        sessionKill(account);

        byte[] sessionKey = sessionKeyGenerate();

        dataSourceManager.query(
            "realmd","INSERT INTO sessions (account, session_key, session_address) VALUES (?, ?, ?)"
        ).configure((query) -> {
            query.setInt(1, account.getAccountId());
            query.setString(2, HexBin.encode(sessionKey));
            query.setString(3, remoteAddress.toString());
        }).executeUpdate();

        logger.debug("Session(user={}, session_key={}): created", account.getLogin(), HexBin.encode(sessionKey));

        return sessionGet(sessionKey);
    }

    public void sessionCleanUp() throws CoreException {
        dataSourceManager.query(
            "realmd","SELECT session_key FROM sessions WHERE last_activity < DATE_SUB(now(), INTERVAL ? SECOND)"
        ).configure((query) -> {
            query.setInt(1, config.getInt("session.ttl", 180));
        }).executeSelect((resultSet) -> {
            while (resultSet.next()) {
                cryptoSessions.remove(resultSet.getString("session_key"));
            }

            return null;
        });

        int killed = dataSourceManager.query(
            "realmd", "DELETE FROM sessions WHERE last_activity < DATE_SUB(now(), INTERVAL ? SECOND)"
        ).configure((query) -> {
            query.setInt(1, config.getInt("session.ttl", 180));
        }).executeUpdate();

        if (killed > 0) {
            logger.info(String.format("Idle sessions killed: %d", killed));
        }
    }

    public Session sessionGet(byte[] sessionKey) throws CoreException {
        return (Session) dataSourceManager.query(
            "realmd", "SELECT sessions.id, accounts.level+0 as level, accounts.login, account FROM sessions " +
                "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
        ).configure((query) -> {
            query.setString(1, HexBin.encode(sessionKey));
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                return new Session(
                    resultSet.getInt("id"),
                    new Account(
                        resultSet.getInt("account"),
                        resultSet.getInt("level"),
                        resultSet.getString("login")
                    ), sessionKey
                );
            }

            return null;
        });
    }

    public Session sessionGet(Account account) throws CoreException {
        if (account == null)
            return null;

        return (Session) dataSourceManager.query(
            "realmd", "SELECT sessions.id, accounts.level+0 as level, accounts.login, account, session_key " +
                "FROM sessions INNER JOIN accounts ON sessions.account = accounts.id WHERE account = ?"
        ).configure((query) -> {
            query.setInt(1, account.getAccountId());
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                return new Session(
                        resultSet.getInt("id"),
                        new Account(
                                resultSet.getInt("account"),
                                resultSet.getInt("level"),
                                resultSet.getString("login")
                        ), HexBin.decode(resultSet.getString("session_key"))
                );
            }

            return null;
        });
    }

    private byte[] sessionKeyGenerate() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }

    public void sessionKill(Account account) throws CoreException {
        dataSourceManager.query(
            "realmd", "DELETE FROM sessions WHERE account = ?"
        ).configure((query) -> {
            query.setInt(1, account.getAccountId());
        }).executeUpdate();
    }

    public void sessionKill(Session session) throws CoreException {
        dataSourceManager.query(
            "realmd", "DELETE FROM sessions WHERE session_key = ?"
        ).configure((query) -> {
            cryptoSessions.remove(session.getKeyHex());

            query.setString(1, HexBin.encode(session.getKey()));
        }).executeUpdate();
    }

    public void sessionUpdateLastActivity(Session session) throws CoreException {
        dataSourceManager.query(
            "realmd", "UPDATE sessions SET last_activity = now() WHERE session_key = ?"
        ).configure((query) -> {
            query.setString(1, HexBin.encode(session.getKey()));
        }).executeUpdate();
    }

    public void sessionUpdateLastActivity(SocketAddress remoteAddress) throws CoreException {
        dataSourceManager.query(
            "realmd", "UPDATE sessions SET last_activity = now() WHERE session_address = ?"
        ).configure((query) -> {
            query.setString(1, remoteAddress.toString());
        }).executeUpdate();
    }
}
