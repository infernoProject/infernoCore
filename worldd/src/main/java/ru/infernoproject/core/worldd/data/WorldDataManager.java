package ru.infernoproject.core.worldd.data;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;

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
