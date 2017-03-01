package ru.infernoproject.core.common.db.sql.impl;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.db.sql.SQLQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLUpdateQuery<T extends SQLObjectWrapper> implements SQLQuery<T> {

    private final DataSourceManager dataSourceManager;
    private final Class<T> objectWrapper;

    private T object;

    public SQLUpdateQuery(DataSourceManager dataSourceManager, Class<T> objectWrapper) {
        this.dataSourceManager = dataSourceManager;
        this.objectWrapper = objectWrapper;
    }

    public SQLUpdateQuery<T> object(T object) {
        this.object = object;

        return this;
    }

    @Override
    public String prepareQuery() {
        try {
            return String.format(
                "UPDATE `%s` SET %s WHERE `id` = %d;",
                T.getTableName(objectWrapper),
                T.prepareSet(objectWrapper, object),
                T.getObjectID(objectWrapper, object)
            );
        } catch (IllegalAccessException e) {
            logger.error("Unable to access ID field on {}: {}", objectWrapper.getSimpleName(), e.getMessage());
        }

        return null;
    }

    @Override
    public List<T> fetchAll() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T fetchOne() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer execute() throws SQLException {
        String sqlQuery = prepareQuery();
        logger.debug("SQLQuery: {}" , sqlQuery);

        if (sqlQuery == null)
            return 0;

        try (Connection connection = dataSourceManager.getConnection(T.getDataBaseName(objectWrapper))) {
            try (PreparedStatement query = connection.prepareStatement(sqlQuery)) {
                return query.executeUpdate();
            }
        }
    }

    @Override
    public Integer executeRaw(String rawQuery) throws SQLException {
        String sqlQuery = String.format(
            "UPDATE `%s` %s", T.getTableName(objectWrapper), rawQuery
        );
        logger.debug("SQLQuery: {}" , sqlQuery);

        try (Connection connection = dataSourceManager.getConnection(T.getDataBaseName(objectWrapper))) {
            try (PreparedStatement query = connection.prepareStatement(sqlQuery)) {
                return query.executeUpdate();
            }
        }
    }
}
