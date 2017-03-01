package ru.infernoproject.core.common.db.sql.impl;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.db.sql.SQLFilter;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.db.sql.SQLQuery;

import com.google.common.base.Joiner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLSelectQuery<T extends SQLObjectWrapper> implements SQLQuery<T> {

    private final DataSourceManager dataSourceManager;
    private final Class<T> objectWrapper;

    private List<SQLFilter> filters = new ArrayList<>();

    private int limit = -1;
    private int offset = -1;

    private String order_by = null;
    private boolean descending = false;

    public SQLSelectQuery(DataSourceManager dataSourceManager, Class<T> objectWrapper) {
        this.dataSourceManager = dataSourceManager;
        this.objectWrapper = objectWrapper;
    }

    public SQLSelectQuery<T> filter(SQLFilter filter) {
        filters.add(filter);
        return this;
    }

    public SQLSelectQuery<T> limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;

        return this;
    }

    public SQLSelectQuery<T> limit(int limit) {
        this.limit = limit;
        this.offset = -1;

        return this;
    }

    public SQLSelectQuery<T> order(String column, boolean descending) {
        this.order_by = column;
        this.descending = descending;

        return this;
    }

    public SQLSelectQuery<T> order(String column) {
        this.order_by = column;
        this.descending = false;

        return this;
    }

    @Override
    public String prepareQuery() {
        List<String> fields = T.listFields(objectWrapper);

        return String.format(
            "SELECT %s FROM `%s`%s%s%s;",
            (fields.size() > 0) ? "`" + Joiner.on("`,`").join(fields) + "`" : "*", T.getTableName(objectWrapper),
            (filters.size() > 0) ? " WHERE " + new SQLFilter().and(filters).toString() : "",
            (order_by != null) ? String.format(" ORDER BY `%s` ", order_by) + ((descending) ? "DESC" : "ASC"): "",
            (limit >= 0) ? " LIMIT " + ((offset >= 0) ? String.format("%d,%d", limit, offset) : String.format("%d", limit)) : ""
        );
    }

    @Override
    public List<T> fetchAll() throws SQLException {
        String sqlQuery = prepareQuery();
        logger.debug("SQLQuery: {}" , sqlQuery);

        try (Connection connection = dataSourceManager.getConnection(T.getDataBaseName(objectWrapper))) {
            try (PreparedStatement query = connection.prepareStatement(sqlQuery)) {
                return T.processResultSet(dataSourceManager, objectWrapper, query.executeQuery());
            }
        }
    }

    public T fetchOne(T object) throws SQLException {
        String sqlQuery = prepareQuery();
        logger.debug("SQLQuery: {}" , sqlQuery);

        try (Connection connection = dataSourceManager.getConnection(T.getDataBaseName(objectWrapper))) {
            try (PreparedStatement query = connection.prepareStatement(sqlQuery)) {
                ResultSet resultSet = query.executeQuery();

                if (!resultSet.next())
                    return null;

                return T.processObject(dataSourceManager, objectWrapper, resultSet, object);
            }
        }
    }

    @Override
    public T fetchOne() throws SQLException {
        List<T> result = this.limit(1).fetchAll();

        if (result.size() > 0) {
            return result.get(0);
        }

        return null;
    }

    @Override
    public Integer execute() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer executeRaw(String query) throws SQLException {
        throw new UnsupportedOperationException();
    }
}