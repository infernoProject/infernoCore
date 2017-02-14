package ru.infernoproject.core.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataBaseQuery {

    private final DataSourceManager dataSourceManager;
    private final String database;
    private final String query;

    private DataBaseQueryConfigurer configurer;

    DataBaseQuery(DataSourceManager dataSourceManager, String database, String query) {
        this.dataSourceManager = dataSourceManager;
        this.database = database;
        this.query = query;
    }

    public DataBaseQuery configure(DataBaseQueryConfigurer configurer) {
        this.configurer = configurer;

        return this;
    }

    public int executeUpdate() throws SQLException {
        try (Connection connection = dataSourceManager.getConnection(database)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (configurer != null) {
                    configurer.configure(statement);
                }

                return statement.executeUpdate();
            }
        }
    }

    public Object executeSelect(DataBaseQueryCallBack callBack) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection(database)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (configurer != null) {
                    configurer.configure(statement);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    return callBack.processResult(resultSet);
                }
            }
        }
    }
}
