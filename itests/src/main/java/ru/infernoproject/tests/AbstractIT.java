package ru.infernoproject.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.client.TestClient;

import java.io.File;
import java.io.IOException;

public class AbstractIT {

    protected TestClient testClient;
    protected ConfigFile config;

    protected int readRetries;
    protected int readTimeOut;

    @BeforeClass(alwaysRun = true)
    protected void readConfig() {
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
    }

    protected TestClient getTestClient(String host, int port) {
        return new TestClient(host, port);
    }

    protected ByteWrapper sendRecv(ByteArray data) throws InterruptedException {
        testClient.send(data);

        return testClient.recv(readRetries, readTimeOut);
    }
}
