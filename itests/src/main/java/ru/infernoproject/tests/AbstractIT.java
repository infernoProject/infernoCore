package ru.infernoproject.tests;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.constants.CommonErrorCodes;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.client.TestClient;
import ru.infernoproject.tests.crypto.CryptoHelper;
import ru.infernoproject.tests.db.DBHelper;

import java.io.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AbstractIT {

    protected TestClient testClient;
    protected ConfigFile config;

    protected DBHelper dbHelper;
    protected CryptoHelper cryptoHelper;

    private byte[] serverSalt;

    protected DataSourceManager dataSourceManager;

    protected static final Logger logger = LoggerFactory.getLogger(AbstractIT.class);

    @BeforeClass(alwaysRun = true)
    protected void setUpConfig() {
        File configFile = new File(System.getProperty("configFile", "testConfig.conf"));

        if (!configFile.exists())
            throw new RuntimeException(String.format("Config file '%s' doesn't exists", configFile.getAbsolutePath()));

        try {
            config = ConfigFile.readConfig(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setUpDataSourceManager();
        getCryptoConfig();

        cryptoHelper = new CryptoHelper(serverSalt);
        dbHelper = new DBHelper(dataSourceManager, cryptoHelper);
    }

    private void setUpDataSourceManager() {
        dataSourceManager = new DataSourceManager(config);
        dataSourceManager.initDataSources("realmd", "world", "characters", "objects");
    }

    private void getCryptoConfig() {
        testClient = getTestClient("realm");
        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));

        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CRYPTO_CONFIG));
        assertThat("Invalid OpCode", response.getByte(), equalTo(RealmOperations.CRYPTO_CONFIG));

        ByteWrapper cryptoConfig = response.getWrapper();
        assertThat("Realm Server should return crypto config", cryptoConfig.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        serverSalt = cryptoConfig.getBytes();
        assertThat("Invalid ServerSalt", serverSalt.length, equalTo(16));

        testClient.disconnect();
        testClient = null;
    }

    protected TestClient getTestClient(String instanceName) {
        return new TestClient(
            config.getString(String.format("%s.server.host", instanceName), "localhost"),
            config.getInt(String.format("%s.server.port", instanceName), 1234)
        );
    }

    protected String getTestResource(String... path) {
        try {
            String fullPath = String.join("/", path);
            InputStream resource = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(fullPath);

            if (resource != null) {
                return IOUtils.toString(new InputStreamReader(resource));
            } else {
                throw new RuntimeException(String.format("Unable to found test resource: %s", fullPath));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
