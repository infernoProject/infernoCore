package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmErrorCodes;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.AbstractIT;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmServerTest extends AbstractIT {

    @BeforeClass(alwaysRun = true)
    public void cleanUpDataBase() {
        dbHelper.cleanUpTable(Account.class);
        dbHelper.cleanUpTable(Session.class);

        dbHelper.cleanUpTable(RealmListEntry.class);

        dbHelper.cleanUpTable(CharacterInfo.class);

        dbHelper.cleanUpTable(RaceInfo.class);
        dbHelper.cleanUpTable(ClassInfo.class);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpTestClient() {
        testClient = getTestClient("realm");

        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));
    }

    @Test(groups = { "IC", "ICRS", "ICRS001" }, description = "RealmServer should register new user")
    public void testCaseICRS001() {
        ByteWrapper response = registerUser("testUserICRS001", "testPassword");
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer should not register existing user")
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

        RaceInfo raceInfo = dbHelper.createRace("Test Race ICRS010", "test_race_icrs010");

        response = getRaceList();
        assertThat("User should receive race list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> raceList = response.getList();
        assertThat("Race list should contain 1 element", raceList.size(), equalTo(1));

        ByteWrapper raceData = raceList.get(0);
        raceData.getInt();

        assertThat("Race name mismatch", raceData.getString(), equalTo(raceInfo.name));
        assertThat("Race resource mismatch", raceData.getString(), equalTo(raceInfo.resource));
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

        ClassInfo classInfo = dbHelper.createClass("Test Class ICRS012", "test_class_icrs012");

        response = getClassList();
        assertThat("User should receive class list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> classList = response.getList();
        assertThat("Class list should contain 1 element", classList.size(), equalTo(1));

        ByteWrapper classData = classList.get(0);
        classData.getInt();

        assertThat("Class name mismatch", classData.getString(), equalTo(classInfo.name));
        assertThat("Class resource mismatch", classData.getString(), equalTo(classInfo.resource));
    }

    @Test(groups = { "IC", "ICRS", "ICRS013" }, description = "RealmServer should not return class list to unauthorized user")
    public void testCaseICRS013() {
        ByteWrapper response;

        response = getClassList();
        assertThat("User should not receive class list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS014" }, description = "RealmServer should create character")
    public void testCaseICRS014() {
        Account user = dbHelper.createUser("testUserICRS014", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS014", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        ClassInfo classInfo = dbHelper.createClass("Test Class ICRS014", "test_class_icrs014");
        RaceInfo raceInfo = dbHelper.createRace("Test Race ICRS014", "test_race_icrs014");

        RealmListEntry realmListEntry = dbHelper.createRealm("Test Realm ICRS014", "icrs014", 8085);

        response = createCharacter(realmListEntry, "testCharacter", "ICRS014", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
        assertThat("Character should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS015" }, description = "RealmServer should not create existing character")
    public void testCaseICRS015() {
        Account user = dbHelper.createUser("testUserICRS015", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS015", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        ClassInfo classInfo = dbHelper.createClass("Test Class ICRS015", "test_class_icrs015");
        RaceInfo raceInfo = dbHelper.createRace("Test Race ICRS015", "test_race_icrs015");

        RealmListEntry realmListEntry = dbHelper.createRealm("Test Realm ICRS015", "icrs015", 8085);

        CharacterInfo characterInfo = dbHelper.createCharacter(user, realmListEntry, "testCharacter", "ICRS015", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

        response = createCharacter(characterInfo.realm, characterInfo.firstName, characterInfo.lastName, characterInfo.gender, characterInfo.raceInfo, characterInfo.classInfo, characterInfo.body);
        assertThat("Character should not be registered", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS016" }, description = "RealmServer should not allow unauthorized user to create character")
    public void testCaseICRS016() {
        ByteWrapper response;

        ClassInfo classInfo = dbHelper.createClass("Test Class ICRS016", "test_class_icrs016");
        RaceInfo raceInfo = dbHelper.createRace("Test Race ICRS016", "test_race_icrs016");

        RealmListEntry realmListEntry = dbHelper.createRealm("Test Realm ICRS016", "icrs016", 8085);

        response = createCharacter(realmListEntry, "testCharacter", "ICRS016", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
        assertThat("Character should not be created", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }


    @Test(groups = { "IC", "ICRS", "ICRS017" }, description = "RealmServer should return character list")
    public void testCaseICRS017() {
        Account user = dbHelper.createUser("testUserICRS017", "testPassword");
        Session session = dbHelper.createSession(user, testClient.getAddress());

        ByteWrapper response = logInStep2(
            "testUserICRS017", "testPassword",
            session.getKey(), session.getVector(), user.getSalt()
        );
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        ClassInfo classInfo = dbHelper.createClass("Test Class ICRS017", "test_class_icrs017");
        RaceInfo raceInfo = dbHelper.createRace("Test Race ICRS017", "test_race_icrs017");

        RealmListEntry realmListEntry = dbHelper.createRealm("Test Realm ICRS017", "icrs017", 8085);

        CharacterInfo characterInfo = dbHelper.createCharacter(user, realmListEntry, "testCharacter", "ICRS017", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

        response = getCharacterList();
        assertThat("Realm Server should return character list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> characterList = response.getList();
        assertThat("Character list should contain 1 element", characterList.size(), equalTo(1));

        ByteWrapper characterData = characterList.get(0);
        int characterId = characterData.getInt();

        assertThat("Character realm mismatch", characterData.getInt(), equalTo(characterInfo.realm.id));
        assertThat("Character first name mismatch", characterData.getString(), equalTo(characterInfo.firstName));
        assertThat("Character last name mismatch", characterData.getString(), equalTo(characterInfo.lastName));
        assertThat("Character race mismatch", characterData.getInt(), equalTo(characterInfo.raceInfo.id));
        assertThat("Character gender mismatch", characterData.getString(), equalTo(characterInfo.gender.toString().toLowerCase()));
        assertThat("Character class mismatch", characterData.getInt(), equalTo(characterInfo.classInfo.id));
    }

    @Test(groups = { "IC", "ICRS", "ICRS018" }, description = "RealmServer should not return character list to unauthorized user")
    public void testCaseICRS018() {
        ByteWrapper response;

        response = getCharacterList();
        assertThat("User should not receive character list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // Realm Server client methods

    private ByteWrapper getCharacterList() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CHARACTER_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_LIST));
        return response.getWrapper();
    }

    private ByteWrapper createCharacter(RealmListEntry realmListEntry, String firstName, String lastName, GenderInfo gender, RaceInfo raceInfo, ClassInfo classInfo, byte[] body) {
        ByteWrapper response = sendRecv(
            new ByteArray(RealmOperations.CHARACTER_CREATE).put(realmListEntry.id)
                .put(firstName).put(lastName).put(gender.toString().toLowerCase())
                .put(raceInfo.id).put(classInfo.id).put(body)
        );

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_CREATE));
        return response.getWrapper();
    }

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
}
