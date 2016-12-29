package ru.infernoproject.core.realmd.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SessionKiller implements Runnable {

    private DataSourceManager dataSourceManager;
    private Integer sessionTtl;

    private static final Logger logger = LoggerFactory.getLogger(SessionKiller.class);

    public SessionKiller(DataSourceManager dataSourceManager, Integer sessionTtl) {
        this.dataSourceManager = dataSourceManager;
        this.sessionTtl = sessionTtl;
    }

    @Override
    public void run() {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            PreparedStatement sessionKiller = connection.prepareStatement(
                "DELETE FROM sessions WHERE last_activity < DATE_SUB(now(), INTERVAL ? SECOND)"
            );

            sessionKiller.setInt(1, sessionTtl);

            int sessionsKilled = sessionKiller.executeUpdate();
            if (sessionsKilled > 0) {
                logger.info(String.format(
                    "Idle sessions killed: %d", sessionsKilled
                ));
            }
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
        }
    }
}
