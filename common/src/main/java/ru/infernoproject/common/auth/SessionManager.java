package ru.infernoproject.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.utils.HexBin;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Random;

public class SessionManager {

    private final DataSourceManager dataSourceManager;
    private final ConfigFile config;

    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    public SessionManager(DataSourceManager dataSourceManager, ConfigFile config) {
        this.dataSourceManager = dataSourceManager;
        this.config = config;
    }

    public Account authorize(Session givenSession, SocketAddress sessionAddress) throws SQLException {
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

    public Session create(Account account, SocketAddress remoteAddress) throws SQLException {
        kill(account);

        byte[] sessionKey = generateKey();

        dataSourceManager.query(Session.class).insert(new Session(
            account, sessionKey, remoteAddress
        ));

        if (logger.isDebugEnabled()) {
            logger.debug("Session(user={}, session_key={}): created", account.getLogin(), HexBin.encode(sessionKey));
        }

        return get(sessionKey);
    }

    public void cleanup() throws SQLException {
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

    public Session get(byte[] sessionKey) throws SQLException {
        return dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter("session_key").eq(HexBin.encode(sessionKey)))
            .fetchOne();
    }

    public Session get(Account account) throws SQLException {
        if (account == null)
            return null;

        return dataSourceManager.query(Session.class).select()
            .filter(new SQLFilter("account").eq(account.getId()))
            .fetchOne();
    }

    private byte[] generateKey() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }

    public void kill(Account account) throws SQLException {
        if (account == null)
            return;

        dataSourceManager.query(Session.class).delete("WHERE `account` = " + account.getId());
    }

    public void kill(Session session) throws SQLException {
        if (session == null)
            return;

        dataSourceManager.query(Session.class).delete("WHERE session_key = '" + session.getKeyHex() + "'");
    }

    public void update(Session session) throws SQLException {
        dataSourceManager.query(Session.class).update("SET last_activity = now() WHERE session_key = '" + session.getKeyHex() + "'");
    }

    public void update(SocketAddress remoteAddress) throws SQLException {
        dataSourceManager.query(Session.class).update("SET last_activity = now() WHERE session_address = '" + remoteAddress.toString() + "'");
    }
}
