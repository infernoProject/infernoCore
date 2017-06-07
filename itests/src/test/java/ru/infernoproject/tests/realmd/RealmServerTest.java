package ru.infernoproject.tests.realmd;

import org.testng.annotations.*;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.AccountBan;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.constants.CommonErrorCodes;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmErrorCodes;
import ru.infernoproject.tests.AbstractIT;
import ru.infernoproject.tests.annotations.Prerequisites;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmServerTest extends AbstractIT {

    private RealmTestClient realmTestClient;

    private Account account;

    private RealmListEntry realmListEntry;

    private RaceInfo raceInfo;
    private ClassInfo classInfo;

    private CharacterInfo characterInfo;

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

        realmTestClient = new RealmTestClient(testClient, cryptoHelper);

        if (testMethod.isAnnotationPresent(Prerequisites.class)) {
            Prerequisites prerequisites = testMethod.getAnnotation(Prerequisites.class);
            List<String> requirements = Arrays.asList(prerequisites.requires());

            if (requirements.contains("user")) {
                account = dbHelper.createUser(testMethod.getName(), "testPassword");
            }

            if (requirements.contains("auth")) {
                if (!requirements.contains("user"))
                    throw new RuntimeException("User required for authentication");

                Session session = dbHelper.createSession(account, testClient.getAddress());

                ByteWrapper response = realmTestClient.logInStep2(
                    testMethod.getName(), "testPassword",
                    session.getKey(), session.getVector(), account.getSalt()
                );
                assertThat("User should pass login challenge", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
            }

            if (requirements.contains("character")) {
                realmListEntry = dbHelper.createRealm(testMethod.getName(), testMethod.getName(), 8085);
                raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
                classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());

                characterInfo = dbHelper.createCharacter(account, realmListEntry, "testCharacter", testMethod.getName(), GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);
            } else {
                if (requirements.contains("realm")) {
                    realmListEntry = dbHelper.createRealm(testMethod.getName(), testMethod.getName(), 8085);
                }

                if (requirements.contains("race")) {
                    raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
                }

                if (requirements.contains("class")) {
                    classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());
                }
            }
        }
     }

    @Test(groups = { "IC", "ICRS", "ICRS001" }, description = "RealmServer should register new user")
    public void testCaseICRS001() {
        ByteWrapper response = realmTestClient.registerUser("testUserICRS001", "testPassword");
        assertThat("User should be registered", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS002" }, description = "RealmServer should not register existing user")
    @Prerequisites(requires = { "user" })
    public void testCaseICRS002() {
        ByteWrapper response = realmTestClient.registerUser(account.login, "testPassword");
        assertThat("User should not be registered", response.getByte(), equalTo(RealmErrorCodes.ALREADY_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS003" }, description = "RealmServer should allow to login with existing user")
    @Prerequisites(requires = { "user" })
    public void testCaseICRS003() {
        ByteWrapper response = realmTestClient.logInStep1(account.login);
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        response = realmTestClient.logInStep2(account.login, "testPassword", response);
        assertThat("User should pass login challenge", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS004" }, description = "RealmServer should not allow to login with not existing user")
    public void testCaseICRS004() {
        ByteWrapper response;

        response = realmTestClient.logInStep1("testUserICRS004");
        assertThat("User should not be able to start login challenge", response.getByte(), equalTo(CommonErrorCodes.AUTH_ERROR));
    }

    @Test(groups = { "IC", "ICRS", "ICRS005" }, description = "RealmServer should not allow to login with invalid password")
    @Prerequisites(requires = { "user" })
    public void testCaseICRS005() {
        ByteWrapper response = realmTestClient.logInStep1(account.login);
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        response = realmTestClient.logInStep2(account.login, "invalidPassword", response);
        assertThat("User should not pass login challenge", response.getByte(), equalTo(RealmErrorCodes.AUTH_INVALID));
    }

    @Test(groups = { "IC", "ICRS", "ICRS006" }, description = "RealmServer should not allow to login if account is banned")
    @Prerequisites(requires = { "user" })
    public void testCaseICRS006() {
        AccountBan ban = dbHelper.banAccount(account, 300, "Just for fun!");

        ByteWrapper response = realmTestClient.logInStep1(account.login);
        assertThat("User should be able to start login challenge", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        response = realmTestClient.logInStep2(account.login, "testPassword", response);
        assertThat("User should not pass login challenge", response.getByte(), equalTo(RealmErrorCodes.USER_BANNED));

        assertThat("Ban reason mismatch", response.getString(), equalTo(ban.reason));
        assertThat("Ban expiration time mismatch", response.getLocalDateTime(), equalTo(ban.expires));
    }

    @Test(groups = { "IC", "ICRS", "ICRS007" }, description = "RealmServer should return session token")
    @Prerequisites(requires = { "user", "auth" })
    public void testCaseICRS007() {
        ByteWrapper response = realmTestClient.getSessionToken();
        assertThat("User should receive session token", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS008" }, description = "RealmServer should not return session token for unauthorized user")
    public void testCaseICRS008() {
        ByteWrapper response;

        response = realmTestClient.getSessionToken();
        assertThat("User should not receive session token", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS009" }, description = "RealmServer should return realm list")
    @Prerequisites(requires = { "user", "auth", "realm" })
    public void testCaseICRS009() {
        ByteWrapper response = realmTestClient.getRealmList();
        assertThat("User should receive realm list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> realmList = response.getList();
        assertThat("Realm list should contain 1 element", realmList.size(), equalTo(1));

        ByteWrapper realmListEntryData = realmList.get(0);
        assertThat("Realm Server name mismatch", realmListEntryData.getString(), equalTo(realmListEntry.name));
        assertThat("Realm Server type mismatch", realmListEntryData.getInt(), equalTo(realmListEntry.type));
        assertThat("Realm Server host mismatch", realmListEntryData.getString(), equalTo(realmListEntry.serverHost));
        assertThat("Realm Server port mismatch", realmListEntryData.getInt(), equalTo(realmListEntry.serverPort));
    }

    @Test(groups = { "IC", "ICRS", "ICRS010" }, description = "RealmServer should not return realm list to unauthorized user")
    public void testCaseICRS010() {
        ByteWrapper response;

        response = realmTestClient.getRealmList();
        assertThat("User should not receive realm list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS011" }, description = "RealmServer should return race list")
    @Prerequisites(requires = { "user", "auth", "race" })
    public void testCaseICRS011() {
        ByteWrapper response = realmTestClient.getRaceList();
        assertThat("User should receive race list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> raceList = response.getList();
        assertThat("Race list should contain 1 element", raceList.size(), equalTo(1));

        ByteWrapper raceData = raceList.get(0);
        raceData.getInt();

        assertThat("Race name mismatch", raceData.getString(), equalTo(raceInfo.name));
        assertThat("Race resource mismatch", raceData.getString(), equalTo(raceInfo.resource));
    }


    @Test(groups = { "IC", "ICRS", "ICRS012" }, description = "RealmServer should not return race list to unauthorized user")
    public void testCaseICRS012() {
        ByteWrapper response;

        response = realmTestClient.getRaceList();
        assertThat("User should not receive race list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS013" }, description = "RealmServer should return class list")
    @Prerequisites(requires = { "user", "auth", "class" })
    public void testCaseICRS013() {
        ByteWrapper response = realmTestClient.getClassList();
        assertThat("User should receive class list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> classList = response.getList();
        assertThat("Class list should contain 1 element", classList.size(), equalTo(1));

        ByteWrapper classData = classList.get(0);
        classData.getInt();

        assertThat("Class name mismatch", classData.getString(), equalTo(classInfo.name));
        assertThat("Class resource mismatch", classData.getString(), equalTo(classInfo.resource));
    }

    @Test(groups = { "IC", "ICRS", "ICRS014" }, description = "RealmServer should not return class list to unauthorized user")
    public void testCaseICRS014() {
        ByteWrapper response;

        response = realmTestClient.getClassList();
        assertThat("User should not receive class list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS015" }, description = "RealmServer should create character")
    @Prerequisites(requires = { "user", "auth", "realm", "race", "class" })
    public void testCaseICRS015() {
        CharacterInfo characterInfo = new CharacterInfo();

        characterInfo.account = account;
        characterInfo.realm = realmListEntry;
        characterInfo.firstName = "testCharacter";
        characterInfo.lastName = "testCaseICRS014";
        characterInfo.gender = GenderInfo.FEMALE;
        characterInfo.raceInfo = raceInfo;
        characterInfo.classInfo = classInfo;
        characterInfo.body = new byte[0];

        ByteWrapper response = realmTestClient.createCharacter(characterInfo);
        assertThat("Character should be registered", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS016" }, description = "RealmServer should not create existing character")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS016() {
        ByteWrapper response = realmTestClient.createCharacter(characterInfo);
        assertThat("Character should not be registered", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS017" }, description = "RealmServer should not allow unauthorized user to create character")
    @Prerequisites(requires = { "realm", "race", "class" })
    public void testCaseICRS017() {
        ByteWrapper response;

        CharacterInfo characterInfo = new CharacterInfo();
        characterInfo.realm = realmListEntry;
        characterInfo.firstName = "testCharacter";
        characterInfo.lastName = "testCaseICRS016";
        characterInfo.gender = GenderInfo.FEMALE;
        characterInfo.raceInfo = raceInfo;
        characterInfo.classInfo = classInfo;
        characterInfo.body = new byte[0];

        response = realmTestClient.createCharacter(characterInfo);
        assertThat("Character should not be created", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS018" }, description = "RealmServer should return character list")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS018() {
        ByteWrapper response = realmTestClient.getCharacterList();
        assertThat("Realm Server should return character list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

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

    @Test(groups = { "IC", "ICRS", "ICRS019" }, description = "RealmServer should not return character list to unauthorized user")
    public void testCaseICRS019() {
        ByteWrapper response;

        response = realmTestClient.getCharacterList();
        assertThat("User should not receive character list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS020" }, description = "RealmServer should delete character")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS020() {
        ByteWrapper response = realmTestClient.deleteCharacter(characterInfo);
        assertThat("Realm Server should delete character", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS021" }, description = "RealmServer should not delete already deleted character")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS021() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = realmTestClient.deleteCharacter(characterInfo);
        assertThat("Realm Server should not delete character", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_DELETED));
    }

    @Test(groups = { "IC", "ICRS", "ICRS022" }, description = "RealmServer should not delete nonexistent character")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS022() {
        dbHelper.deleteCharacter(characterInfo, true);

        ByteWrapper response = realmTestClient.deleteCharacter(characterInfo);
        assertThat("Realm Server should not delete character", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_NOT_FOUND));
    }

    @Test(groups = { "IC", "ICRS", "ICRS023" }, description = "RealmServer should not allow unauthorized user to delete character")
    public void testCaseICRS023() {
        ByteWrapper response;

        CharacterInfo characterInfo = new CharacterInfo();

        characterInfo.id = new Random().nextInt();

        response = realmTestClient.deleteCharacter(characterInfo);
        assertThat("User should not delete character", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = { "IC", "ICRS" , "ICRS024" }, description = "Realm Server should return list of deleted characters")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS024() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = realmTestClient.getRestoreableCharacterList();
        assertThat("Realm Server should return character list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

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

    @Test(groups = { "IC", "ICRS" , "ICRS025" }, description = "Realm Server should restore deleted characters")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS025() {
        dbHelper.deleteCharacter(characterInfo);

        ByteWrapper response = realmTestClient.restoreCharacter(characterInfo);
        assertThat("Character should be restored", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = { "IC", "ICRS" , "ICRS026" }, description = "Realm Server should not restore deleted characters with name equal to existing one")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS026() {
        dbHelper.deleteCharacter(characterInfo);
        dbHelper.createCharacter(characterInfo);

        ByteWrapper response = realmTestClient.restoreCharacter(characterInfo);
        assertThat("Character should not be restored", response.getByte(), equalTo(RealmErrorCodes.CHARACTER_EXISTS));
    }

    @Test(groups = { "IC", "ICRS", "ICRS027" }, description = "Realm Server should confirm charatcer selection")
    @Prerequisites(requires = { "user", "auth", "character" })
    public void testCaseICRS027() {
        ByteWrapper response = realmTestClient.selectCharacter(characterInfo);
        assertThat("Character should be selected", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        account = null;
        realmListEntry = null;
        raceInfo = null;
        classInfo = null;
        characterInfo = null;

        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }
}
