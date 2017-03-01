package ru.infernoproject.common.db.sql.impl;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.SQLQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLDeleteQuery<T extends SQLObjectWrapper> implements SQLQuery<T> {

    private final DataSourceManager dataSourceManager;
    private final Class<T> objectWrapper;

    private T object;

    public SQLDeleteQuery(DataSourceManager dataSourceManager, Class<T> objectWrapper) {
        this.dataSourceManager = dataSourceManager;
        this.objectWrapper = objectWrapper;
    }


    public SQLDeleteQuery<T> object(T object) {
        this.object = object;

        return this;
    }


    @Override
    public String prepareQuery() {
        return String.format(
            "DELETE FROM `%s` WHERE `id` = %d;",
            T.getTableName(objectWrapper),
            T.getObjectID(objectWrapper, object)
        );
    }

    @Override
    public Integer execute() throws SQLException {
        return dataSourceManager.executeUpdate(
            T.getDataBaseName(objectWrapper), prepareQuery()
        );
    }

    @Override
    public Integer executeRaw(String rawQuery) throws SQLException {
        return dataSourceManager.executeUpdate(
            T.getDataBaseName(objectWrapper), String.format(
                "DELETE FROM `%s` %s", T.getTableName(objectWrapper), rawQuery
            )
        );
    }
}
