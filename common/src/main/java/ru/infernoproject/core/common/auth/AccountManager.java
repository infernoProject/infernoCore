package ru.infernoproject.core.common.auth;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerSession;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.config.ConfigFile;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.types.auth.Session;
import ru.infernoproject.core.common.srp.SRP6Engine;
import ru.infernoproject.core.common.types.auth.LogInStep1Challenge;
import ru.infernoproject.core.common.types.auth.LogInStep2Challenge;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public Account accountCreate(String login, BigInteger salt, BigInteger verifier) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            Account account = accountGet(login);

            if (account == null) {
                PreparedStatement insertQuery = connection.prepareStatement(
                    "INSERT INTO accounts (login, level, salt, verifier) VALUES (?, ?, ?, ?)"
                );

                insertQuery.setString(1, login);
                insertQuery.setInt(2, 1);
                insertQuery.setString(3, HexBin.encode(salt.toByteArray()));
                insertQuery.setString(4, HexBin.encode(verifier.toByteArray()));

                insertQuery.execute();

                return accountGet(login);
            }
        }

        return null;
    }

    public Account accountGet(String login) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement accountQuery = connection.prepareStatement(
                "SELECT id, level+0 as level, login FROM accounts WHERE login = ?"
            );

            accountQuery.setString(1, login);

            try (ResultSet resultSet = accountQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new Account(
                        resultSet.getInt("id"),
                        resultSet.getInt("level"),
                        resultSet.getString("login")
                    );
                }
            }
        }

        return null;
    }

    public LogInStep1Challenge accountLogInStep1(SocketAddress remoteAddress, String login) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement query = connection.prepareStatement(
                "SELECT id, login, salt, verifier, level+0 as level FROM accounts WHERE login = ?"
            );
            query.setString(1, login);

            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    BigInteger salt = new BigInteger(HexBin.decode(resultSet.getString("salt")));
                    BigInteger verifier = new BigInteger(HexBin.decode(resultSet.getString("verifier")));

                    SRP6ServerSession cryptoSession = srp6Engine.getSession();

                    BigInteger B = cryptoSession.step1(
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

                    return new LogInStep1Challenge(session, salt, B);
                }
            }
        }

        return new LogInStep1Challenge();
    }

    public LogInStep2Challenge accountLogInStep2(Session session, BigInteger A, BigInteger M1) throws SRP6Exception {
        SRP6ServerSession cryptoSession = cryptoSessions.get(session.getKeyHex());

        if (cryptoSession.getState().equals(SRP6ServerSession.State.STEP_1)) {
            BigInteger M2 = cryptoSession.step2(A, M1);

            return new LogInStep2Challenge(M2);
        }

        return new LogInStep2Challenge();
    }

    public SRP6CryptoParams cryptoParamsGet() {
        return srp6Engine.getCryptoParams();
    }

    public Account sessionAuthorize(Session session, SocketAddress sessionAddress) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT sessions.account, accounts.level+0 as level, accounts.login FROM sessions " +
                    "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
            );

            sessionQuery.setString(1, HexBin.encode(session.getKey()));

            try (ResultSet resultSet = sessionQuery.executeQuery()) {
                if (resultSet.next()) {
                    PreparedStatement sessionSetter = connection.prepareStatement(
                        "UPDATE sessions SET session_address = ? WHERE session_key = ?"
                    );

                    sessionSetter.setString(1, sessionAddress.toString());
                    sessionSetter.setString(2, HexBin.encode(session.getKey()));

                    sessionSetter.execute();

                    return new Account(
                        resultSet.getInt("account"),
                        resultSet.getInt("level"),
                        resultSet.getString("login")
                    );
                }
            }
        }

        return null;
    }

    public Session sessionCreate(Account account, SocketAddress remoteAddress) throws SQLException {
        sessionKill(account);

        byte[] sessionKey = sessionKeyGenerate();

        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionCreator = connection.prepareStatement(
                "INSERT INTO sessions (account, session_key, session_address) VALUES (?, ?, ?)"
            );

            sessionCreator.setInt(1, account.getAccountId());
            sessionCreator.setString(2, HexBin.encode(sessionKey));
            sessionCreator.setString(3, remoteAddress.toString());

            sessionCreator.execute();
        }

        logger.debug("Session(user={}, session_key={}): created", account.getLogin(), HexBin.encode(sessionKey));

        return sessionGet(sessionKey);
    }

    public void sessionCleanUp() throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT session_key FROM sessions WHERE last_activity < DATE_SUB(now(), INTERVAL ? SECOND)"
            );

            sessionQuery.setInt(1, config.getInt("session.ttl", 180));

            try (ResultSet resultSet = sessionQuery.executeQuery()) {
                while (resultSet.next()) {
                    cryptoSessions.remove(resultSet.getString("session_key"));
                }
            }

            PreparedStatement sessionKiller = connection.prepareStatement(
                "DELETE FROM sessions WHERE last_activity < DATE_SUB(now(), INTERVAL ? SECOND)"
            );

            sessionKiller.setInt(1, config.getInt("session.ttl", 180));
            int sessionsKilled = sessionKiller.executeUpdate();

            if (sessionsKilled > 0) {
                logger.info(String.format(
                    "Idle sessions killed: %d", sessionsKilled
                ));
            }
        }
    }

    public Session sessionGet(byte[] sessionKey) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT sessions.id, accounts.level+0 as level, accounts.login, account FROM sessions " +
                    "INNER JOIN accounts ON sessions.account = accounts.id WHERE session_key = ?"
            );

            sessionQuery.setString(1, HexBin.encode(sessionKey));

            try (ResultSet resultSet = sessionQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new Session(
                        resultSet.getInt("id"),
                        new Account(
                            resultSet.getInt("account"),
                            resultSet.getInt("level"),
                            resultSet.getString("login")
                        ),
                        sessionKey
                    );
                }
            }
        }

        return null;
    }

    public Session sessionGet(Account account) throws SQLException {
        if (account == null)
            return null;

        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionQuery = connection.prepareStatement(
                "SELECT sessions.id, accounts.level+0 as level, accounts.login, account, session_key " +
                    "FROM sessions INNER JOIN accounts ON sessions.account = accounts.id WHERE account = ?"
            );

            sessionQuery.setInt(1, account.getAccountId());

            try (ResultSet resultSet = sessionQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new Session(
                        resultSet.getInt("id"),
                        new Account(
                            resultSet.getInt("account"),
                            resultSet.getInt("level"),
                            resultSet.getString("login")
                        ),
                        HexBin.decode(resultSet.getString("session_key"))
                    );
                }
            }
        }

        return null;
    }

    public byte[] sessionKeyGenerate() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }

    public void sessionKill(Account account) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionKiller = connection.prepareStatement(
                "DELETE FROM sessions WHERE account = ?"
            );

            sessionKiller.setInt(1, account.getAccountId());
            sessionKiller.execute();
        }
    }

    public void sessionKill(Session session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionKiller = connection.prepareStatement(
                "DELETE FROM sessions WHERE session_key = ?"
            );

            cryptoSessions.remove(session.getKeyHex());

            sessionKiller.setString(1, HexBin.encode(session.getKey()));
            sessionKiller.execute();
        }
    }

    public void sessionUpdateLastActivity(Session session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionUpdater = connection.prepareStatement(
                "UPDATE sessions SET last_activity = now() WHERE session_key = ?"
            );

            sessionUpdater.setString(1, HexBin.encode(session.getKey()));
            sessionUpdater.execute();
        }
    }

    public void sessionUpdateLastActivity(SocketAddress remoteAddress) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionUpdater = connection.prepareStatement(
                "UPDATE sessions SET last_activity = now() WHERE session_address = ?"
            );

            sessionUpdater.setString(1, remoteAddress.toString());
            sessionUpdater.execute();
        }
    }
}
