package ru.infernoproject.core.common.db.sql.impl;

import com.google.common.base.Joiner;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.db.sql.SQLQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SQLInsertQuery<T extends SQLObjectWrapper> implements SQLQuery<T> {

    private final DataSourceManager dataSourceManager;
    private final Class<T> objectWrapper;

    private List<T> values = new ArrayList<>();

    public SQLInsertQuery(DataSourceManager dataSourceManager, Class<T> objectWrapper) {
        this.dataSourceManager = dataSourceManager;
        this.objectWrapper = objectWrapper;
    }

    public SQLInsertQuery<T> values(T... values) {
        Collections.addAll(this.values, values);

        return this;
    }

    @Override
    public String prepareQuery() {
        return String.format(
            "INSERT INTO `%s` (%s) VALUES (%s);",
            T.getTableName(objectWrapper), "`" + Joiner.on("`,`").join(T.listFields(objectWrapper, true)) + "`",
            Joiner.on("),(").join(
                values.stream()
                    .map(value -> T.prepareValue(objectWrapper, value))
                    .collect(Collectors.toList())
            )
        );
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

        try (Connection connection = dataSourceManager.getConnection(T.getDataBaseName(objectWrapper))) {
            try (PreparedStatement query = connection.prepareStatement(sqlQuery)) {
                return query.executeUpdate();
            }
        }
    }

    @Override
    public Integer executeRaw(String query) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
