package ru.infernoproject.core.worldd;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.net.ServerSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WorldSession implements ServerSession {

    private byte[] sessionKey;

    private Integer accountID;
    private Integer accessLevel;
    private Integer characterID;

    private boolean authorized = false;

    private final DataSourceManager dataSourceManager;

    private Logger logger = LoggerFactory.getLogger(WorldSession.class);

    public WorldSession(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public Integer getAccountID() {
        return accountID;
    }

    public void setAccountID(Integer accountID) {
        this.accountID = accountID;
    }

    public Integer getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(Integer accessLevel) {
        this.accessLevel = accessLevel;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Integer getCharacterID() {
        return characterID;
    }

    public void setCharacterID(Integer characterID) {
        this.characterID = characterID;
    }

    public void update() {
        if (isAuthorized()) {
            try (Connection connection = dataSourceManager.getConnection("realmd")) {
                PreparedStatement sessionUpdater = connection.prepareStatement(
                    "UPDATE sessions SET last_activity = now() WHERE session_key = ?"
                );

                sessionUpdater.setString(1, HexBin.encode(sessionKey));

                sessionUpdater.execute();
            } catch (SQLException e) {
                logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            }
        }
    }

    public void close() throws SQLException {
        setAuthorized(false);

        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionKiller = connection.prepareStatement(
                "DELETE FROM sessions WHERE session_key = ?"
            );

            sessionKiller.setString(1, HexBin.encode(sessionKey));

            sessionKiller.execute();
        }
    }
}
