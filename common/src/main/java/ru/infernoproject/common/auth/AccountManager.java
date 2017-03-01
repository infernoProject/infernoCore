package ru.infernoproject.common.auth;

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerSession;
import ru.infernoproject.common.utils.HexBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.srp.SRP6Engine;
import ru.infernoproject.common.auth.sql.LogInStep1Challenge;
import ru.infernoproject.common.auth.sql.LogInStep2Challenge;

import java.math.BigInteger;
import java.net.SocketAddress;
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

    public Account accountCreate(String login, String email, BigInteger salt, BigInteger verifier) throws SQLException {
        Account account = accountGet(login);

        if (account == null) {
            dataSourceManager.query(Account.class).insert(new Account(
                login, "user", email, salt, verifier
            ));

            return accountGet(login);
        }

        return null;
    }

    public Account accountGet(String login) throws SQLException {
        return dataSourceManager.query(Account.class).select()
            .filter(new SQLFilter("login").eq(login))
            .fetchOne();
    }

    public LogInStep1Challenge accountLogInStep1(SocketAddress remoteAddress, String login) throws SQLException {
        Account account = accountGet(login);

        if (account != null) {
            BigInteger salt = account.getSalt();
            BigInteger verifier = account.getVerifier();

            SRP6ServerSession cryptoSession = srp6Engine.getSession();

            BigInteger b = cryptoSession.step1(account.getLogin(), salt, verifier);
            Session session = sessionCreate(account, remoteAddress);

            cryptoSessions.put(session.getKeyHex(), cryptoSession);

            return new LogInStep1Challenge(session, salt, b);
        }

        return new LogInStep1Challenge();
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

    public Account sessionAuthorize(Session givenSession, SocketAddress sessionAddress) throws SQLException {
        Session session = dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter("session_key").eq(HexBin.encode(givenSession.getKey())))
            .fetchOne();

        if (session != null) {
            session.setAddress(sessionAddress);

            dataSourceManager.query(Session.class).update(session);

            return session.getAccount();
        }

        return null;
    }

    public Session sessionCreate(Account account, SocketAddress remoteAddress) throws SQLException {
        sessionKill(account);

        byte[] sessionKey = sessionKeyGenerate();

        dataSourceManager.query(Session.class).insert(new Session(
            account, sessionKey, remoteAddress
        ));

        logger.debug("Session(user={}, session_key={}): created", account.getLogin(), HexBin.encode(sessionKey));

        return sessionGet(sessionKey);
    }

    public void sessionCleanUp() throws SQLException {
        dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter().raw("`last_activity` < DATE_SUB(now(), INTERVAL " + config.getInt("session.ttl", 180) + " SECOND)"))
            .fetchAll().parallelStream().forEach(session -> {
                cryptoSessions.remove(session.getKeyHex());

                try {
                    dataSourceManager.query(Session.class).delete(session);
                } catch (SQLException e) {
                    logger.error("Unable to delete session: {}", e.getMessage());
                }
            });
    }

    public Session sessionGet(byte[] sessionKey) throws SQLException {
        return dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter("session_key").eq(HexBin.encode(sessionKey)))
            .fetchOne();
    }

    public Session sessionGet(Account account) throws SQLException {
        if (account == null)
            return null;

        return dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter("account").eq(account.getAccountId()))
            .fetchOne();
    }

    private byte[] sessionKeyGenerate() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }

    public void sessionKill(Account account) throws SQLException {
        dataSourceManager.query(Session.class).delete("WHERE `account` = " + account.getAccountId());
    }

    public void sessionKill(Session session) throws SQLException {
        dataSourceManager.query(Session.class).delete("WHERE session_key = '" + session.getKeyHex() + "'");
    }

    public void sessionUpdateLastActivity(Session session) throws SQLException {
        dataSourceManager.query(Session.class).update("SET last_activity = now() WHERE session_key = '" + session.getKeyHex() + "'");
    }

    public void sessionUpdateLastActivity(SocketAddress remoteAddress) throws SQLException {
        dataSourceManager.query(Session.class).update("SET last_activity = now() WHERE session_address = '" + remoteAddress.toString() + "'");
    }
}
