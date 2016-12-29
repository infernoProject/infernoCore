package ru.infernoproject.core.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import ru.infernoproject.core.common.config.ConfigFile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DataSourceManager {

    private final ConfigFile config;
    private final Map<String, DataSource> dataSources;

    public DataSourceManager(ConfigFile config) {
        this.config = config;
        this.dataSources = new HashMap<>();
    }

    public void initDataSources(String... dataSourceNames) {
        for (String dataSourceName: dataSourceNames) {
            dataSources.put(dataSourceName, initDataSource(dataSourceName));
        }
    }

    private HikariConfig getDataSourceConfig(String dataSourceName) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(
            this.config.getString(String.format("jdbc.%s.url", dataSourceName), null)
        );
        config.setUsername(
            this.config.getString(String.format("jdbc.%s.username", dataSourceName), null)
        );
        config.setPassword(
            this.config.getString(String.format("jdbc.%s.password", dataSourceName), null)
        );

        return config;
    }

    private DataSource initDataSource(String dataSourceName) {
        HikariDataSource dataSource = new HikariDataSource(
            getDataSourceConfig(dataSourceName)
        );

        Flyway migrationManager = new Flyway();
        migrationManager.setDataSource(dataSource);
        migrationManager.setLocations(String.format(
            "classpath:db/migration/%s", dataSourceName
        ));

        if (Boolean.getBoolean(System.getProperty("reInitDataBase", "false"))) {
            migrationManager.clean();
        }

        migrationManager.migrate();

        return dataSource;
    }

    public Connection getConnection(String dataSource) throws SQLException {
        if (!dataSources.containsKey(dataSource)) {
            throw new IllegalArgumentException(
                String.format("Unknown DataSource: %s", dataSource)
            );
        }

        return dataSources.get(dataSource).getConnection();
    }
}
