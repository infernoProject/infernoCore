package ru.infernoproject.tests.worldd;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.AbstractIT;
import ru.infernoproject.worldd.constants.WorldErrorCodes;
import ru.infernoproject.worldd.constants.WorldOperations;

import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class WorldServerTest extends AbstractIT {

    @BeforeClass(alwaysRun = true)
    public void cleanUpDataBase() {
        dbHelper.cleanUpTable(Account.class);
        dbHelper.cleanUpTable(Session.class);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpTestClient() {
        testClient = getTestClient("world");
        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));
    }

    @Test(groups = {"IC", "ICWS", "ICWS001"}, description = "World Server should authorize session")
    public void testCaseICWS001() {
        Account user = dbHelper.createUser("testUserICWS001", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = authorize(session.getKey());
        assertThat("World Server should authorize session", response.getByte(), equalTo(WorldErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS002"}, description = "World Server should not authorize invalid session")
    public void testCaseICWS002() {
        byte[] invalidSession = new byte[16];
        new Random().nextBytes(invalidSession);

        ByteWrapper response = authorize(invalidSession);
        assertThat("World Server not should authorize invalid session", response.getByte(), equalTo(WorldErrorCodes.AUTH_ERROR));
    }

    @Test(groups = {"IC", "ICWS", "ICWS003"}, description = "World Server should allow to log out")
    public void testCaseICWS003() {
        Account user = dbHelper.createUser("testUserICWS003", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = authorize(session.getKey());
        assertThat("World Server should authorize session", response.getByte(), equalTo(WorldErrorCodes.SUCCESS));

        response = logOut();
        assertThat("World Server should allow to log out", response.getByte(), equalTo(WorldErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS004"}, description = "World Server should not allow to log out for unauthorized users")
    public void testCaseICWS004() {
        ByteWrapper response = logOut();
        assertThat("World Server should not allow to log out", response.getByte(), equalTo(WorldErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = {"IC", "ICWS", "ICWS005"}, description = "World Server should respond to heartbeat")
    public void testCaseICWS005() {
        ByteWrapper response = heartBeat();
        assertThat("World Server should respond to heart beat", response.getByte(), equalTo(WorldErrorCodes.SUCCESS));

        long delay = System.currentTimeMillis() - response.getLong();
        assertThat("World Server should respond in time", delay, new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object o) {
                return ((Long) o) <= 1000L ;
            }

            @Override
            public void describeTo(Description description) {

            }
        });
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // World Client methods

    private ByteWrapper authorize(byte[] session) {
        ByteWrapper response = sendRecv(new ByteArray(WorldOperations.AUTHORIZE).put(session));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.AUTHORIZE));

        return response.getWrapper();
    }

    private ByteWrapper logOut() {
        ByteWrapper response = sendRecv(new ByteArray(WorldOperations.LOG_OUT));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.LOG_OUT));

        return response.getWrapper();
    }

    private ByteWrapper heartBeat() {
        ByteWrapper response = sendRecv(new ByteArray(WorldOperations.HEART_BEAT).put(System.currentTimeMillis()));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.HEART_BEAT));

        return response.getWrapper();
    }
}
