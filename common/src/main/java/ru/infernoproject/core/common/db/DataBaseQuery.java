package ru.infernoproject.core.common.db;

import ru.infernoproject.core.common.error.CoreException;

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

    public int executeUpdate() throws CoreException {
        try (Connection connection = dataSourceManager.getConnection(database)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (configurer != null) {
                    configurer.configure(statement);
                }

                return statement.executeUpdate();
            }
        } catch (Exception e) {
            throw new CoreException(e);
        }
    }

    public Object executeSelect(DataBaseQueryCallBack callBack) throws CoreException {
        try (Connection connection = dataSourceManager.getConnection(database)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (configurer != null) {
                    configurer.configure(statement);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    return callBack.processResult(resultSet);
                }
            }
        } catch (Exception e) {
            throw new CoreException(e);
        }
    }
}
