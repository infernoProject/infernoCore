package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmErrorCodes;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.AbstractIT;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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

    @Test(groups = { "IC", "ICRS", "ICRS001" }, description = "RealmServer should provide CryptoConfig")
    public void testCaseICRS001() {
        byte[] serverSalt = getCryptoConfig();
        assertThat("Invalid ServerSalt", serverSalt.length, equalTo(16));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer should register new user")
    public void testCaseICRS002() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response = registerUser(
            "testUserICRS002", "testUserICRS002@testCase", "testPassword", serverSalt
        );

        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS003" }, description = "RealmServer shouldn't register existing user")
    public void testCaseICRS003() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
            "testUserICRS003", "testUserICRS003@testCase", "testPassword", serverSalt
        );
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = registerUser(
            "testUserICRS003", "testUserICRS003@testCase", "testPassword", serverSalt
        );
        assertThat("User should not be registered", response.getByte(), equalTo(RealmErrorCodes.ALREADY_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS004" }, description = "RealmServer should allow to login with existing user")
    public void testCaseICRS004() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
            "testUserICRS004", "testUserICRS004@testCase", "testPassword", serverSalt
        );
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep1("testUserICRS004");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS004", "testPassword", serverSalt, response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS005" }, description = "RealmServer should not allow to login with not existing user")
    public void testCaseICRS005() {
        ByteWrapper response;

        response = logInStep1("testUserICRS005");
        assertThat("User should not be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_ERROR));
    }

    @Test(groups = { "IC", "ICRS", "ICRS006" }, description = "RealmServer should not allow to login with invalid password")
    public void testCaseICRS006() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
            "testUserICRS006", "testUserICRS006@testCase", "testPassword", serverSalt
        );
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep1("testUserICRS006");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS006", "invalidPassword", serverSalt, response);
        assertThat("User should not pass login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_INVALID));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should return session token")
    public void testCaseICRS007() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
            "testUserICRS007", "testUserICRS007@testCase", "testPassword", serverSalt
        );
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep1("testUserICRS007");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS007", "testPassword", serverSalt, response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = getSessionToken();
        assertThat("User should receive session token", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS008" }, description = "RealmServer should not return session token for unauthorized user")
    public void testCaseICRS008() {
        ByteWrapper response;

        response = getSessionToken();
        assertThat("User should not receive session token", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS009" }, description = "RealmServer should return realm list")
    public void testCaseICRS009() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
                "testUserICRS009", "testUserICRS009@testCase", "testPassword", serverSalt
        );
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep1("testUserICRS009");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS009", "testPassword", serverSalt, response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = getRealList();
        assertThat("User should receive realm list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> realmList = response.getList();
        assertThat("Realm list should contain 1 element", realmList.size(), equalTo(1));

        ByteWrapper realmServer = realmList.get(0);
        assertThat("Realm Server name mismatch", realmServer.getString(), equalTo("Test World"));
        assertThat("Realm Server type mismatch", realmServer.getInt(), equalTo(1));
        assertThat("Realm Server host mismatch", realmServer.getString(), equalTo(config.getString("world.server.host", "localhost")));
        assertThat("Realm Server port mismatch", realmServer.getInt(), equalTo(config.getInt("world.server.port", 8085)));
    }

    @Test(groups = { "IC", "ICRS", "ICRS010" }, description = "RealmServer should not return realm list to unauthorized user")
    public void testCaseICRS010() {
        ByteWrapper response;

        response = getRealList();
        assertThat("User should not receive realm list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // Realm Server client methods

    private ByteWrapper getRealList() {
        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.REALM_LIST));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.REALM_LIST));
            return response.getWrapper();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteWrapper getSessionToken() {
        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.SESSION_TOKEN));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SESSION_TOKEN));
            return response.getWrapper();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteWrapper logInStep1(String login) {
        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.LOG_IN_STEP1).put(login));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP1));
            return response.getWrapper();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteWrapper logInStep2(String login, String password, byte[] serverSalt, ByteWrapper loginChallenge) {
        byte[] sessionKey = loginChallenge.getBytes();

        byte[] clientSalt = loginChallenge.getBytes();
        byte[] vector = loginChallenge.getBytes();

        byte[] challenge;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            md.update(vector);
            md.update(calculateVerifier(login, password, serverSalt, clientSalt));
            md.update(serverSalt);

            challenge = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.LOG_IN_STEP2).put(sessionKey).put(challenge));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP2));
            return response.getWrapper();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteWrapper registerUser(String login, String email, String password, byte[] serverSalt) {
        byte[] clientSalt = generateSalt();

        byte[] clientVerifier = calculateVerifier(login, password, serverSalt, clientSalt);

        try {
            ByteWrapper response = sendRecv(
                new ByteArray(RealmOperations.SIGN_UP)
                    .put(login).put(email)
                    .put(clientSalt).put(clientVerifier)
            );

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SIGN_UP));

            return response.getWrapper();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getCryptoConfig() {
        try {
            ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CRYPTO_CONFIG));

            assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CRYPTO_CONFIG));
            response = response.getWrapper();

            assertThat("Should be success", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
            return response.getBytes();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return salt;
    }

    private byte[] calculateVerifier(String login, String password, byte[] serverSalt, byte[] clientSalt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            md.update(serverSalt);
            md.update(String.format("%s:%s", login, password).getBytes());
            md.update(clientSalt);

            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
