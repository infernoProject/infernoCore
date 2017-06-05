package ru.infernoproject.common.data;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.sql.SQLFilter;

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

    public RaceInfo raceGetById(int raceId) throws SQLException {
        return dataSourceManager.query(RaceInfo.class).select()
            .filter(new SQLFilter("id").eq(raceId))
            .fetchOne();
    }

    public List<ClassInfo> classList() throws SQLException {
        return dataSourceManager.query(ClassInfo.class).select().fetchAll();
    }

    public ClassInfo classGetById(int classId) throws SQLException {
        return dataSourceManager.query(ClassInfo.class).select()
            .filter(new SQLFilter("id").eq(classId))
            .fetchOne();
    }

    public void update(Long diff) {
        // TODO(aderyugin): Implement object expiration
    }
}
