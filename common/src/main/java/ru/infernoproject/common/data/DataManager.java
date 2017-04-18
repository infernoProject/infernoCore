package ru.infernoproject.common.data;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.RaceInfo;

import java.sql.SQLException;
import java.util.List;

public class DataManager {

    private final DataSourceManager dataSourceManager;

    public DataManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<RaceInfo> raceList() throws SQLException {
        return dataSourceManager.query(RaceInfo.class).select().fetchAll();
    }

    public List<ClassInfo> classList() throws SQLException {
        return dataSourceManager.query(ClassInfo.class).select().fetchAll();
    }

    public void update(Long diff) {
        // TODO(aderyugin): Implement object expiration
    }
}
