package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmErrorCodes;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.AbstractIT;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        assertThat("Invalid ServerSalt", serverSalt.length, equalTo(21));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer should register new user")
    public void testCaseICRS002() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response = registerUser(
            "testUserICRS002", "testUserICRS002@testCase", "testPassword", serverSalt
        );

        assertThat("Invalid status code", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS003" }, description = "RealmServer shouldn't register existing user")
    public void testCaseICRS003() {
        byte[] serverSalt = getCryptoConfig();
        ByteWrapper response;

        response = registerUser(
            "testUserICRS003", "testUserICRS003@testCase", "testPassword", serverSalt
        );
        assertThat("Invalid status code", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = registerUser(
            "testUserICRS003", "testUserICRS003@testCase", "testPassword", serverSalt
        );
        assertThat("Invalid status code", response.getByte(), equalTo(RealmErrorCodes.ALREADY_EXISTS));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // Realm Server client methods

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
