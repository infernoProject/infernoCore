package ru.infernoproject.common.realmlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;

import java.sql.SQLException;
import java.util.List;

public class RealmList {

    private final DataSourceManager dataSourceManager;
    private static final Logger logger = LoggerFactory.getLogger(RealmList.class);

    public RealmList(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RealmListEntry> list() throws SQLException {
        return dataSourceManager.query(RealmListEntry.class).select()
            .filter(new SQLFilter("online").eq(1))
            .fetchAll();
    }

    public RealmListEntry get(String serverName) throws SQLException {
        return dataSourceManager.query(RealmListEntry.class).select()
            .filter(new SQLFilter("name").eq(serverName))
            .fetchOne();
    }

    public RealmListEntry get(int serverid) throws SQLException {
        return dataSourceManager.query(RealmListEntry.class).select()
            .filter(new SQLFilter("id").eq(serverid))
            .fetchOne();
    }

    public int online(String serverName, boolean onLine) throws SQLException {
        String query = onLine ? "SET `last_seen` = NOW(), `online` = 1 WHERE `name` = '" + serverName + "'" : "SET `online` = 0 WHERE `name` = '" + serverName + "'";

        return dataSourceManager.query(RealmListEntry.class)
            .update(query);
    }

    public void check() throws SQLException {
        dataSourceManager.query(RealmListEntry.class).select()
            .filter(new SQLFilter().raw("`last_seen` < DATE_SUB(now(), INTERVAL 30 SECOND) AND `online` = 1"))
            .fetchAll().parallelStream().forEach(serverInfo -> {
                try {
                    online(serverInfo.name, false);
                    logger.info("Server '{}' has gone off-line", serverInfo.name);
                } catch (SQLException e) {
                    logger.error("Unable to update server status: {}", e.getMessage());
                }
            });
    }

    public boolean exists(String serverName) throws SQLException {
        return get(serverName) != null;
    }
}
