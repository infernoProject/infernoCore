package ru.infernoproject.core.realmd;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RealmList {

    private final DataSourceManager dataSourceManager;

    public RealmList(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RealmServerInfo> listRealmServers() throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            List<RealmServerInfo> realmList = new ArrayList<>();
            try (PreparedStatement realmServerList = connection.prepareStatement(
                "SELECT name, type, server_host, server_port FROM realm_list WHERE online = 1"
            )) {
                try (ResultSet resultSet = realmServerList.executeQuery()) {
                    while (resultSet.next()) {
                        RealmServerInfo realmServerInfo = new RealmServerInfo(
                            resultSet.getString("name"),
                            resultSet.getInt("type"),
                            resultSet.getString("server_host"),
                            resultSet.getInt("server_port")
                        );

                        realmList.add(realmServerInfo);
                    }
                }
            }

            return realmList;
        }
    }
}
