package ru.infernoproject.core.worldd.data;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.error.CoreException;
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

    @SuppressWarnings("unchecked")
    public List<RaceInfo> raceList() throws CoreException {
        return (List<RaceInfo>) dataSourceManager.query(
            "world", "SELECT id, name, resource FROM races"
        ).executeSelect((resultSet -> {
            List<RaceInfo> raceList = new ArrayList<>();

            while (resultSet.next()) {
                raceList.add(new RaceInfo(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("resource")
                ));
            }

            return raceList;
        }));
    }

    @SuppressWarnings("unchecked")
    public List<ClassInfo> classList() throws CoreException {
        return (List<ClassInfo>) dataSourceManager.query(
            "world", "SELECT id, name, resource FROM classes"
        ).executeSelect((resultSet -> {
            List<ClassInfo> classList = new ArrayList<>();

            while (resultSet.next()) {
                classList.add(new ClassInfo(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("resource")
                ));
            }

            return classList;
        }));
    }

    public void update(Long diff) {

    }
}
