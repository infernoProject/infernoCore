package ru.infernoproject.tests;

import org.testng.annotations.BeforeClass;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.client.TestClient;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class AbstractIT {

    protected TestClient testClient;
    protected ConfigFile config;

    protected int readRetries;
    protected int readTimeOut;

    protected DataSourceManager dataSourceManager;

    @BeforeClass(alwaysRun = true)
    protected void setUp() {
        File configFile = new File(System.getProperty("config.file", "testConfig.conf"));

        if (!configFile.exists())
            throw new RuntimeException(String.format("Config file '%s' doesn't exists", configFile.getAbsolutePath()));

        try {
            config = ConfigFile.readConfig(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        readTimeOut = config.getInt("tests.read_timeout", 3000);
        readRetries = config.getInt("tests.read_retries", 10);

        dataSourceManager = new DataSourceManager(config);
        dataSourceManager.initDataSources("realmd", "world", "characters", "objects");
    }

    protected TestClient getTestClient(String host, int port) {
        return new TestClient(host, port);
    }

    protected <T extends SQLObjectWrapper> void cleanUpTable(Class<T> model) {
        try {
            dataSourceManager.query(model).delete("");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected ByteWrapper sendRecv(ByteArray data) {
        testClient.send(data);

        try {
            return testClient.recv(readRetries, readTimeOut);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
