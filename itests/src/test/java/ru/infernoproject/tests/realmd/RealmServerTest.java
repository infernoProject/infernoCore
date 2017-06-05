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
import ru.infernoproject.tests.annotations.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmServerTest extends AbstractIT {

    Account account;
    Session session;

    RealmListEntry realmListEntry;

    RaceInfo raceInfo;
    ClassInfo classInfo;

    CharacterInfo characterInfo;

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
    public void setUpTestClient(Method testMethod) {
        testClient = getTestClient("realm");
        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));

        if (testMethod.isAnnotationPresent(AuthRequired.class)) {
            account = dbHelper.createUser(testMethod.getName(), "testPassword");
            session = dbHelper.createSession(account, testClient.getAddress());

            ByteWrapper response = logInStep2(
                testMethod.getName(), "testPassword",
                session.getKey(), session.getVector(), account.getSalt()
            );
            assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
        } else if (testMethod.isAnnotationPresent(SessionRequired.class)) {
            account = dbHelper.createUser(testMethod.getName(), "testPassword");
            session = dbHelper.createSession(account, testClient.getAddress());
        } else if (testMethod.isAnnotationPresent(UserRequired.class)) {
            account = dbHelper.createUser(testMethod.getName(), "testPassword");
        }

        if (testMethod.isAnnotationPresent(CharacterRequired.class)) {
            realmListEntry = dbHelper.createRealm(testMethod.getName(), testMethod.getName(), 8085);
            raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
            classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());
            characterInfo = dbHelper.createCharacter(account, realmListEntry, "testCharacter", testMethod.getName(), GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
        } else {
            if (testMethod.isAnnotationPresent(RealmRequired.class)) {
                realmListEntry = dbHelper.createRealm(testMethod.getName(), testMethod.getName(), 8085);
            }

            if (testMethod.isAnnotationPresent(RaceRequired.class)) {
                raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
            }

            if (testMethod.isAnnotationPresent(ClassRequired.class)) {
                classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());
            }
        }
     }

    @Test(groups = { "IC", "ICRS", "ICRS001" }, description = "RealmServer should register new user")
    public void testCaseICRS001() {
        ByteWrapper response = registerUser("testUserICRS001", "testPassword");
        assertThat("User should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer should not register existing user")
    @UserRequired
    public void testCaseICRS002() {
        ByteWrapper response = registerUser(account.login, "testPassword");
        assertThat("User should not be registered", response.getByte(), equalTo(RealmErrorCodes.ALREADY_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS003" }, description = "RealmServer should allow to login with existing user")
    @UserRequired
    public void testCaseICRS003() {
        ByteWrapper response = logInStep1(account.login);
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2(account.login, "testPassword", response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS004" }, description = "RealmServer should not allow to login with not existing user")
    public void testCaseICRS004() {
        ByteWrapper response;

        response = logInStep1("testUserICRS004");
        assertThat("User should not be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_ERROR));
    }

    @Test(groups = { "IC", "ICRS", "ICRS005" }, description = "RealmServer should not allow to login with invalid password")
    @UserRequired
    public void testCaseICRS005() {
        ByteWrapper response = logInStep1(account.login);
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        response = logInStep2(account.login, "invalidPassword", response);
        assertThat("User should not pass login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_INVALID));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should return session token")
    @AuthRequired
    public void testCaseICRS006() {
        ByteWrapper response = getSessionToken();
        assertThat("User should receive session token", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should not return session token for unauthorized user")
    public void testCaseICRS007() {
        ByteWrapper response;

        response = getSessionToken();
        assertThat("User should not receive session token", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS008" }, description = "RealmServer should return realm list")
    @AuthRequired
    @RealmRequired
    public void testCaseICRS008() {
        ByteWrapper response = getRealmList();
        assertThat("User should receive realm list", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));

        List<ByteWrapper> realmList = response.getList();
        assertThat("Realm list should contain 1 element", realmList.size(), equalTo(1));

        ByteWrapper realmListEntryData = realmList.get(0);
        assertThat("Realm Server name mismatch", realmListEntryData.getString(), equalTo(realmListEntry.name));
        assertThat("Realm Server type mismatch", realmListEntryData.getInt(), equalTo(realmListEntry.type));
        assertThat("Realm Server host mismatch", realmListEntryData.getString(), equalTo(realmListEntry.serverHost));
        assertThat("Realm Server port mismatch", realmListEntryData.getInt(), equalTo(realmListEntry.serverPort));
    }

    @Test(groups = { "IC", "ICRS", "ICRS009" }, description = "RealmServer should not return realm list to unauthorized user")
    public void testCaseICRS009() {
        ByteWrapper response;

        response = getRealmList();
        assertThat("User should not receive realm list", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS010" }, description = "RealmServer should return race list")
    @AuthRequired
    @RaceRequired
    public void testCaseICRS010() {
        ByteWrapper response = getRaceList();
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
    @AuthRequired
    @ClassRequired
    public void testCaseICRS012() {
        ByteWrapper response = getClassList();
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
    @AuthRequired
    @RealmRequired
    @ClassRequired
    @RaceRequired
    public void testCaseICRS014() {
        ByteWrapper response = createCharacter(realmListEntry, "testCharacter", "ICRS014", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
        assertThat("Character should be registered", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS015" }, description = "RealmServer should not create existing character")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS015() {
        ByteWrapper response = createCharacter(characterInfo.realm, characterInfo.firstName, characterInfo.lastName, characterInfo.gender, characterInfo.raceInfo, characterInfo.classInfo, characterInfo.body);
        assertThat("Character should not be registered", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS016" }, description = "RealmServer should not allow unauthorized user to create character")
    @ClassRequired
    @RealmRequired
    public void testCaseICRS016() {
        ByteWrapper response;

        response = createCharacter(realmListEntry, "testCharacter", "ICRS016", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
        assertThat("Character should not be created", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS017" }, description = "RealmServer should return character list")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS017() {
        ByteWrapper response = getCharacterList();
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

    @Test(groups = { "IC", "ICRS", "ICRS019" }, description = "RealmServer should delete character")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS019() {
        ByteWrapper response = deleteCharacter(characterInfo);
        assertThat("Realm Server should delete character", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS020" }, description = "RealmServer should not delete already deleted character")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS020() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = deleteCharacter(characterInfo);
        assertThat("Realm Server should not delete character", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_DELETED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS021" }, description = "RealmServer should not delete nonexistent character")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS021() {
        dbHelper.deleteCharacter(characterInfo, true);

        ByteWrapper response = deleteCharacter(characterInfo);
        assertThat("Realm Server should not delete character", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_NOT_FOUND));
    }

    @Test(groups = { "IC", "ICRS", "ICRS022" }, description = "RealmServer should not allow unauthorized user to delete character")
    public void testCaseICRS022() {
        ByteWrapper response;

        CharacterInfo characterInfo = new CharacterInfo();

        characterInfo.id = new Random().nextInt();

        response = deleteCharacter(characterInfo);
        assertThat("User should not delete character", response.getByte(), equalTo(RealmErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS" , "ICRS023" }, description = "Realm Server should return list of deleted characters")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS023() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = getRestoreableCharacterList();
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

    @Test(groups = { "IC", "ICRS" , "ICRS024" }, description = "Realm Server should restore deleted characters")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS024() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = restoreCharacter(characterInfo);
        assertThat("Character should be restored", response.getByte(), equalTo(RealmErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS" , "ICRS025" }, description = "Realm Server should not restore deleted characters with name equal to existing one")
    @AuthRequired
    @CharacterRequired
    public void testCaseICRS025() {
        dbHelper.deleteCharacter(characterInfo);
        dbHelper.createCharacter(account, realmListEntry, characterInfo.firstName, characterInfo.lastName, GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

        ByteWrapper response = restoreCharacter(characterInfo);
        assertThat("Character should not be restored", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_EXISTS));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }

    // Realm Server client methods

    private ByteWrapper restoreCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CHARACTER_RESTORE).put(characterInfo.id));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_RESTORE));
        return response.getWrapper();
    }


    private ByteWrapper getRestoreableCharacterList() {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CHARACTER_RESTOREABLE_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_RESTOREABLE_LIST));
        return response.getWrapper();
    }

    private ByteWrapper deleteCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = sendRecv(new ByteArray(RealmOperations.CHARACTER_DELETE).put(characterInfo.id));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_DELETE));
        return response.getWrapper();
    }

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
