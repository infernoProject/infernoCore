package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmErrorCodes;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.AbstractIT;
import ru.infernoproject.tests.crypto.CryptoHelper;
import ru.infernoproject.tests.db.DBHelper;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmServerTest extends AbstractIT {

    private DBHelper dbHelper;
    private CryptoHelper cryptoHelper;

    @BeforeClass(alwaysRun = true)
    public void cleanUpDataBase() {
        cleanUpTable(Account.class);
        cleanUpTable(Session.class);

        cleanUpTable(RealmListEntry.class);

        cleanUpTable(CharacterInfo.class);

        cleanUpTable(RaceInfo.class);
        cleanUpTable(ClassInfo.class);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpTestClient() {
        testClient = getTestClient(
            config.getString("realm.server.host", "127.0.0.1"),
            config.getInt("realm.server.port", 3274)
        );

        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));

        byte[] serverSalt = getCryptoConfig();
        assertThat("Invalid ServerSalt", serverSalt.length, equalTo(16));

        cryptoHelper = new CryptoHelper(serverSalt);
        dbHelper = new DBHelper(dataSourceManager, cryptoHelper);
    }

    @Test(groups = { "IC", "ICRS", "ICRS001" }, description = "RealmServer should register new user")
    public void testCaseICRS001() {
        ByteWrapper response = registerUser("testUserICRS001", "testPassword");
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer shouldn't register existing user")
    public void testCaseICRS002() {
        dbHelper.createUser("testUserICRS002", "testPassword");
        ByteWrapper response = registerUser("testUserICRS002", "testPassword");
        assertThat("User should not be registered", response.getByte(), equalTo(RealmErrorCodes.ALREADY_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS003" }, description = "RealmServer should allow to login with existing user")
    public void testCaseICRS003() {
        dbHelper.createUser("testUserICRS003", "testPassword");

        ByteWrapper response = logInStep1("testUserICRS003");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS003", "testPassword", response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS004" }, description = "RealmServer should not allow to login with not existing user")
    public void testCaseICRS004() {
        ByteWrapper response;

        response = logInStep1("testUserICRS004");
        assertThat("User should not be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_ERROR));
    }

    @Test(groups = { "IC", "ICRS", "ICRS005" }, description = "RealmServer should not allow to login with invalid password")
    public void testCaseICRS005() {
        dbHelper.createUser("testUserICRS005", "testPassword");

        ByteWrapper response = logInStep1("testUserICRS005");
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2("testUserICRS005", "invalidPassword", response);
        assertThat("User should not pass login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_INVALID));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should return session token")
    public void testCaseICRS006() {
        Account user = dbHelper.createUser("testUserICRS006", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS006", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = getSessionToken();
        assertThat("User should receive session token", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should not return session token for unauthorized user")
    public void testCaseICRS007() {
        ByteWrapper response;

        response = getSessionToken();
        assertThat("User should not receive session token", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS008" }, description = "RealmServer should return realm list")
    public void testCaseICRS008() {
        Account user = dbHelper.createUser("testUserICRS008", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS008", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        RealmListEntry realmServer = dbHelper.createRealm(
            "Test World",
            config.getString("world.server.host", "localhost"),
            config.getInt("world.server.port", 8085)
        );

        response = getRealmList();
        assertThat("User should receive realm list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> realmList = response.getList();
        assertThat("Realm list should contain 1 element", realmList.size(), equalTo(1));

        ByteWrapper realmListEntry = realmList.get(0);
        assertThat("Realm Server name mismatch", realmListEntry.getString(), equalTo(realmServer.name));
        assertThat("Realm Server type mismatch", realmListEntry.getInt(), equalTo(realmServer.type));
        assertThat("Realm Server host mismatch", realmListEntry.getString(), equalTo(realmServer.serverHost));
        assertThat("Realm Server port mismatch", realmListEntry.getInt(), equalTo(realmServer.serverPort));
    }

    @Test(groups = { "IC", "ICRS", "ICRS009" }, description = "RealmServer should not return realm list to unauthorized user")
    public void testCaseICRS009() {
        ByteWrapper response;

        response = getRealmList();
        assertThat("User should not receive realm list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS010" }, description = "RealmServer should return race list")
    public void testCaseICRS010() {
        Account user = dbHelper.createUser("testUserICRS010", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS010", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        dbHelper.createRace("Test Race", "test_race");

        response = getRaceList();
        assertThat("User should receive race list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> raceList = response.getList();
        assertThat("Race list should contain 1 element", raceList.size(), equalTo(1));

        ByteWrapper raceInfo = raceList.get(0);
        raceInfo.getInt();

        assertThat("Race name mismatch", raceInfo.getString(), equalTo("Test Race"));
        assertThat("Race resource mismatch", raceInfo.getString(), equalTo("test_race"));
    }


    @Test(groups = { "IC", "ICRS", "ICRS011" }, description = "RealmServer should not return race list to unauthorized user")
    public void testCaseICRS011() {
        ByteWrapper response;

        response = getRaceList();
        assertThat("User should not receive race list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS012" }, description = "RealmServer should return class list")
    public void testCaseICRS012() {
        Account user = dbHelper.createUser("testUserICRS012", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS012", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        dbHelper.createClass("Test Class", "test_class");

        response = getClassList();
        assertThat("User should receive class list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> classList = response.getList();
        assertThat("Class list should contain 1 element", classList.size(), equalTo(1));

        ByteWrapper classInfo = classList.get(0);
        classInfo.getInt();

        assertThat("Class name mismatch", classInfo.getString(), equalTo("Test Class"));
        assertThat("Class resource mismatch", classInfo.getString(), equalTo("test_class"));
    }

    @Test(groups = { "IC", "ICRS", "ICRS013" }, description = "RealmServer should not return class list to unauthorized user")
    public void testCaseICRS013() {
        ByteWrapper response;

        response = getClassList();
        assertThat("User should not receive class list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // Realm Server client methods

    private ByteWrapper getClassList() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CLASS_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CLASS_LIST));
        return response.getWrapper();
    }

    private ByteWrapper getRaceList() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.RACE_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.RACE_LIST));
        return response.getWrapper();
    }

    private ByteWrapper getRealmList() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.REALM_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.REALM_LIST));
        return response.getWrapper();
    }

    private ByteWrapper getSessionToken() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.SESSION_TOKEN));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SESSION_TOKEN));
        return response.getWrapper();
    }

    private ByteWrapper logInStep1(String login) {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.LOG_IN_STEP1).put(login));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP1));
        return response.getWrapper();
    }

    private ByteWrapper logInStep2(String login, String password, ByteWrapper loginChallenge) {
        byte[] sessionKey = loginChallenge.getBytes();

        byte[] clientSalt = loginChallenge.getBytes();
        byte[] vector = loginChallenge.getBytes();

        return logInStep2(login, password, sessionKey, vector, clientSalt);
    }

    private ByteWrapper logInStep2(String login, String password, byte[] sessionKey, byte[] vector, byte[] clientSalt) {
        byte[] challenge = cryptoHelper.calculateChallenge(login, password, vector, clientSalt);

        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.LOG_IN_STEP2).put(sessionKey).put(challenge));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP2));
        return response.getWrapper();
    }

    private ByteWrapper registerUser(String login, String password) {
        byte[] clientSalt = cryptoHelper.generateSalt();
        byte[] clientVerifier = cryptoHelper.calculateVerifier(login, password, clientSalt);

        ByteWrapper response = sendRecv(
            new ByteArray(RealmOperations.SIGN_UP)
                .put(login).put(String.format("%s@testCase", login))
                .put(clientSalt).put(clientVerifier)
        );

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SIGN_UP));

        return response.getWrapper();
    }

    private byte[] getCryptoConfig() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CRYPTO_CONFIG));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CRYPTO_CONFIG));
        response = response.getWrapper();

        assertThat("Should be success", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
        return response.getBytes();
    }
}
