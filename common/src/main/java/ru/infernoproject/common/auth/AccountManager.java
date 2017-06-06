package ru.infernoproject.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.utils.HexBin;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;

import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;

public class AccountManager {

    private final DataSourceManager dataSourceManager;
    private final SessionManager sessionManager;

    private final byte[] serverSalt;

    private static final Logger logger = LoggerFactory.getLogger(AccountManager.class);

    public AccountManager(DataSourceManager dataSourceManager, SessionManager sessionManager, ConfigFile config) {
        this.dataSourceManager = dataSourceManager;
        this.sessionManager = sessionManager;

        this.serverSalt = config.getHexBytes("crypto.salt", HexBin.decode("d41d8cd98f00b204e9800998ecf8427e"));
    }

    public byte[] serverSalt() {
        return serverSalt;
    }

    public Account create(String login, String email, byte[] salt, byte[] verifier) throws SQLException {
        Account account = get(login);

        if (account == null) {
            dataSourceManager.query(Account.class).insert(new Account(
                login, AccountLevel.USER, email, salt, verifier
            ));

            return get(login);
        }

        return null;
    }

    public Account get(String login) throws SQLException {
        return dataSourceManager.query(Account.class).select()
            .filter(new SQLFilter("login").eq(login))
            .fetchOne();
    }

    public Session logInStep1(SocketAddress remoteAddress, String login) throws SQLException {
        Account account = get(login);

        if (account != null) {
            return sessionManager.create(account, remoteAddress);
        }

        return null;
    }

    public boolean logInStep2(Session session, byte[] challenge) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        messageDigest.update(session.getVector());
        messageDigest.update(session.getAccount().getVerifier());
        messageDigest.update(serverSalt);

        byte[] digest = messageDigest.digest();

        if (logger.isDebugEnabled()) {
            logger.debug("S_SALT: {}", HexBin.encode(serverSalt));
            logger.debug("VERIFIER: {}", HexBin.encode(session.getAccount().getVerifier()));
            logger.debug("VECTOR: {}", HexBin.encode(session.getVector()));
            logger.debug("CHLG: {} <==> {}", HexBin.encode(digest), HexBin.encode(challenge));
        }

        return Arrays.equals(digest, challenge);
    }
}
