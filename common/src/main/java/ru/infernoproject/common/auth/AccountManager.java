package ru.infernoproject.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.common.utils.HexBin;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;

import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

public class AccountManager {

    private final DataSourceManager dataSourceManager;
    private final ConfigFile config;

    private final byte[] serverSalt;

    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(AccountManager.class);

    public AccountManager(DataSourceManager dataSourceManager, ConfigFile config) {
        this.dataSourceManager = dataSourceManager;
        this.config = config;

        this.serverSalt = config.getHexBytes("crypto.salt", HexBin.decode("d41d8cd98f00b204e9800998ecf8427e"));
    }

    public byte[] getServerSalt() {
        return serverSalt;
    }

    public Account accountCreate(String login, String email, byte[] salt, byte[] verifier) throws SQLException {
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

    public Session accountLogInStep1(SocketAddress remoteAddress, String login) throws SQLException {
        Account account = accountGet(login);

        if (account != null) {
            return sessionCreate(account, remoteAddress);
        }

        return null;
    }

    public boolean accountLogInStep2(Session session, byte[] challenge) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        logger.debug("S_SALT: {}", HexBin.encode(serverSalt));
        logger.debug("VERIFIER: {}", HexBin.encode(session.getAccount().getVerifier()));
        logger.debug("VECTOR: {}", HexBin.encode(session.getVector()));

        messageDigest.update(session.getVector());
        messageDigest.update(session.getAccount().getVerifier());
        messageDigest.update(serverSalt);

        byte[] digest = messageDigest.digest();

        logger.debug("CHLG: {} <==> {}", HexBin.encode(digest), HexBin.encode(challenge));

        return Arrays.equals(digest, challenge);
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
            .filter(new SQLFilter("account").eq(account.getId()))
            .fetchOne();
    }

    private byte[] sessionKeyGenerate() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }

    public void sessionKill(Account account) throws SQLException {
        dataSourceManager.query(Session.class).delete("WHERE `account` = " + account.getId());
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
