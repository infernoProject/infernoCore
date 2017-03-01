package ru.infernoproject.common.db.sql;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.impl.SQLDeleteQuery;
import ru.infernoproject.common.db.sql.impl.SQLInsertQuery;
import ru.infernoproject.common.db.sql.impl.SQLSelectQuery;
import ru.infernoproject.common.db.sql.impl.SQLUpdateQuery;

import java.sql.SQLException;

public class SQLQueryBuilder<T extends SQLObjectWrapper> {

    private final DataSourceManager dataSourceManager;
    private final Class<T> objectWrapper;

    public SQLQueryBuilder(DataSourceManager dataSourceManager, Class<T> object) {
        this.dataSourceManager = dataSourceManager;
        this.objectWrapper = object;
    }

    public SQLSelectQuery<T> select() {
        return new SQLSelectQuery<>(dataSourceManager, objectWrapper);
    }

    public Integer insert(T... objects) throws SQLException {
        return new SQLInsertQuery<>(dataSourceManager, objectWrapper).values(objects).execute();
    }

    public Integer update(T object) throws SQLException {
        return new SQLUpdateQuery<>(dataSourceManager, objectWrapper).object(object).execute();
    }

    public Integer update(String query) throws SQLException {
        return new SQLUpdateQuery<>(dataSourceManager, objectWrapper).executeRaw(query);
    }

    public Integer delete(T object) throws SQLException {
        return new SQLDeleteQuery<>(dataSourceManager, objectWrapper).object(object).execute();
    }

    public Integer delete(String query) throws SQLException {
        return new SQLDeleteQuery<>(dataSourceManager, objectWrapper).executeRaw(query);
    }
}
