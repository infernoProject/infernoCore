package ru.infernoproject.realmd;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;

import java.sql.SQLException;
import java.util.List;

public class RealmList {

    private final DataSourceManager dataSourceManager;

    public RealmList(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RealmServerInfo> listRealmServers() throws SQLException {
        return dataSourceManager.query(RealmServerInfo.class).select()
            .filter(new SQLFilter("online").eq(1))
            .fetchAll();
    }
}
