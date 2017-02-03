package ru.infernoproject.core.worldd.data;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorldDataManager {

    private final DataSourceManager dataSourceManager;

    public WorldDataManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RaceInfo> raceList() throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            try (PreparedStatement raceQuery = connection.prepareStatement(
                "SELECT id, name, resource FROM races"
            )) {

                List<RaceInfo> raceList = new ArrayList<>();
                try (ResultSet resultSet = raceQuery.executeQuery()) {
                    while (resultSet.next()) {
                        raceList.add(new RaceInfo(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("resource")
                        ));
                    }
                }

                return raceList;
            }
        }
    }

    public List<ClassInfo> classList() throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            try (PreparedStatement classQuery = connection.prepareStatement(
                "SELECT id, name, resource FROM classes"
            )) {
                List<ClassInfo> classList = new ArrayList<>();
                try (ResultSet resultSet = classQuery.executeQuery()) {
                    while (resultSet.next()) {
                        classList.add(new ClassInfo(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("resource")
                        ));
                    }
                }

                return classList;
            }
        }
    }

    public void update(Long diff) {

    }
}
