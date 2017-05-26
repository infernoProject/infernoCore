package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.AbstractIT;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmServerTest extends AbstractIT {

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        testClient = getTestClient(
            config.getString("realm.server.host", "127.0.0.1"),
            config.getInt("realm.server.port", 3274)
        );

        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));
    }

    @Test(groups = { "IC", "ICRS", "ICRS001" })
    public void testCaseICRS001() {
        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CRYPTO_CONFIG));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CRYPTO_CONFIG));
            assertThat("Invalid ServerSalt", response.getBytes().length, equalTo(21));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }
}
