package ru.infernoproject.core.realmd;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.error.CoreException;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;

import java.util.ArrayList;
import java.util.List;

public class RealmList {

    private final DataSourceManager dataSourceManager;

    public RealmList(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @SuppressWarnings("unchecked")
    public List<RealmServerInfo> listRealmServers() throws CoreException {
        return (List<RealmServerInfo>) dataSourceManager.query(
            "realmd", "SELECT name, type, server_host, server_port FROM realm_list WHERE online = 1"
        ).executeSelect((resultSet) -> {
            List<RealmServerInfo> realmList = new ArrayList<>();

            while (resultSet.next()) {
                RealmServerInfo realmServerInfo = new RealmServerInfo(
                    resultSet.getString("name"),
                    resultSet.getInt("type"),
                    resultSet.getString("server_host"),
                    resultSet.getInt("server_port")
                );

                realmList.add(realmServerInfo);
            }

            return realmList;
        });
    }
}
