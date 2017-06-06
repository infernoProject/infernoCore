package ru.infernoproject.common.db.sql.impl;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.SQLQuery;

import com.google.common.base.Joiner;

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

    private String group_by = null;

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

    public SQLSelectQuery<T> group(String... columns) {
        this.group_by = Joiner.on("`,`").join(columns);

        return this;
    }

    @Override
    public String prepareQuery() {
        List<String> fields = T.listFields(objectWrapper);

        return String.format(
            "SELECT %s FROM `%s`%s%s%s%s;",
            !fields.isEmpty() ? Joiner.on(",").join(fields) : "*", T.getTableName(objectWrapper),
            !filters.isEmpty() ? " WHERE " + new SQLFilter().and(filters).toString() : "",
            (order_by != null) ? String.format(" ORDER BY `%s` ", order_by) + (descending ? "DESC" : "ASC"): "",
            (limit >= 0) ? " LIMIT " + ((offset >= 0) ? String.format("%d,%d", limit, offset) : String.format("%d", limit)) : "",
            (group_by != null) ? String.format(" GROUP BY `%s`", group_by) : ""
        );
    }

    @Override
    public List<T> fetchAll() throws SQLException {
        return dataSourceManager.executeSelect(
            objectWrapper, T.getDataBaseName(objectWrapper), prepareQuery()
        );
    }

    @Override
    public T fetchOne() throws SQLException {
        List<T> result = this.limit(1).fetchAll();

        if (!result.isEmpty()) {
            return result.get(0);
        }

        return null;
    }
}