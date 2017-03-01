package ru.infernoproject.worldd.data;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.worldd.data.sql.ClassInfo;
import ru.infernoproject.worldd.data.sql.RaceInfo;

import java.sql.SQLException;
import java.util.List;

public class WorldDataManager {

    private final DataSourceManager dataSourceManager;

    public WorldDataManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RaceInfo> raceList() throws SQLException {
        return dataSourceManager.query(RaceInfo.class).select().fetchAll();
    }

    public List<ClassInfo> classList() throws SQLException {
        return dataSourceManager.query(ClassInfo.class).select().fetchAll();
    }

    public void update(Long diff) {

    }
}
