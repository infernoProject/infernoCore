package ru.infernoproject.tests.worldd;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.constants.CommonErrorCodes;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.oid.OID;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.AbstractIT;
import ru.infernoproject.tests.annotations.Prerequisites;
import ru.infernoproject.tests.client.TestClient;
import ru.infernoproject.worldd.constants.WorldErrorCodes;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;
import ru.infernoproject.worldd.script.sql.Spell;
import ru.infernoproject.worldd.script.sql.SpellType;
import ru.infernoproject.worldd.world.chat.ChatMessageType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class WorldServerTest extends AbstractIT {

    private TestClient testClient2;

    private WorldTestClient worldTestClient;
    private WorldTestClient worldTestClient2;

    private Account account;
    private Account account2;

    private Session session;
    private Session session2;

    private CharacterInfo character;
    private CharacterInfo character2;

    @BeforeClass(alwaysRun = true)
    public void cleanUpDataBase() {
        dbHelper.cleanUpTable(Account.class, "WHERE login LIKE 'testCase%'");
        dbHelper.cleanUpTable(Session.class, "");

        dbHelper.cleanUpTable(RealmListEntry.class, "WHERE name LIKE 'testCase%'");

        dbHelper.cleanUpTable(CharacterInfo.class, "WHERE first_name = 'testCharacter'");

        dbHelper.cleanUpTable(RaceInfo.class, "WHERE name LIKE 'testCase%'");
        dbHelper.cleanUpTable(ClassInfo.class, "WHERE name LIKE 'testCase%'");

        dbHelper.cleanUpTable(Command.class, "");
        dbHelper.cleanUpTable(Script.class, "");
        dbHelper.cleanUpTable(Spell.class, "");
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpTestClient(Method testMethod) {
        testClient = getTestClient("world");
        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));

        worldTestClient = new WorldTestClient(testClient);

        if (testMethod.isAnnotationPresent(Prerequisites.class)) {
            Prerequisites prerequisites = testMethod.getAnnotation(Prerequisites.class);
            List<String> requirements = Arrays.asList(prerequisites.requires());

            if (requirements.contains("2nd_player")) {
                testClient2 = getTestClient("world");
                assertThat("Unable to connect to server", testClient2.isConnected(), equalTo(true));

                worldTestClient2 = new WorldTestClient(testClient2);
            }

            if (requirements.contains("session")) {
                account = dbHelper.createUser(testMethod.getName(), "testPassword");
                session = dbHelper.createSession(account, testClient.getAddress());

                if (requirements.contains("2nd_player")) {
                    account2 = dbHelper.createUser(testMethod.getName() + "_2", "testPassword");
                    session2 = dbHelper.createSession(account2, testClient2.getAddress());
                }
            }

            if (requirements.contains("admin")) {
                if (!requirements.contains("session"))
                    throw new RuntimeException("Session required");

                dbHelper.setUserAccessLevel(account, AccountLevel.ADMIN);

                if (requirements.contains("2nd_player")) {
                    dbHelper.setUserAccessLevel(account2, AccountLevel.ADMIN);
                }
            }

            if (requirements.contains("character")) {
                if (!requirements.contains("session"))
                    throw new RuntimeException("Session required for character creation");

                RaceInfo raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
                ClassInfo classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());

                RealmListEntry realmListEntry = dbHelper.createRealmIfNotExists("testWorld", "testWorld", 8085);
                character = dbHelper.createCharacter(account, realmListEntry, "testCharacter", testMethod.getName(), GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

                dbHelper.selectCharacter(session, character);

                if (requirements.contains("2nd_player")) {
                    character2 = dbHelper.createCharacter(account2, realmListEntry, "testCharacter", testMethod.getName() + "_2", GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

                    dbHelper.selectCharacter(session2, character2);
                }
            }

            if (requirements.contains("auth")) {
                if (!requirements.contains("character"))
                    throw new RuntimeException("Character required for authentication");

                ByteWrapper response = worldTestClient.authorize(session.getKey());
                assertThat("World Server should authorize session", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
            }
        }
    }

    @Test(groups = {"IC", "ICWS", "ICWS_AUTH"}, description = "World Server should authorize session")
    @Prerequisites(requires = { "session", "character" })
    public void testCaseICWS001() {
        ByteWrapper response = worldTestClient.authorize(session.getKey());
        assertThat("World Server should authorize session", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_AUTH"}, description = "World Server should not authorize invalid session")
    public void testCaseICWS002() {
        byte[] invalidSession = new byte[16];
        new Random().nextBytes(invalidSession);

        ByteWrapper response = worldTestClient.authorize(invalidSession);
        assertThat("World Server not should authorize invalid session", response.getByte(), equalTo(CommonErrorCodes.AUTH_ERROR));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_AUTH"}, description = "World Server should allow to log out")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS003() {
        ByteWrapper response = worldTestClient.logOut();
        assertThat("World Server should allow to log out", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_AUTH"}, description = "World Server should not allow to log out for unauthorized users")
    public void testCaseICWS004() {
        ByteWrapper response = worldTestClient.logOut();
        assertThat("World Server should not allow to log out", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_MISC"}, description = "World Server should respond to heartbeat")
    public void testCaseICWS005() {
        ByteWrapper response = worldTestClient.heartBeat();
        assertThat("World Server should respond to heart beat", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

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

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should execute command")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS006() {
        Command command = dbHelper.createCommand("icws006", AccountLevel.USER, "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.executeCommand(command.name, "arg1", "arg2");
        assertThat("World Server should execute command", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        ByteWrapper result = response.getWrapper();
        String[] echoArguments = result.getStrings();

        assertThat("Command result is invalid", echoArguments, equalTo(new String[] { "arg1", "arg2" }));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should not execute command if user has not enough access level")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS007() {
        Command command = dbHelper.createCommand("icws007", AccountLevel.ADMIN, "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.executeCommand(command.name, "arg1", "arg2");
        assertThat("World Server should not execute command", response.getByte(), equalTo(CommonErrorCodes.AUTH_ERROR));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should not execute nonexistent command")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS008() {
        ByteWrapper response = worldTestClient.executeCommand("nonexistent", "arg1", "arg2");
        assertThat("World Server should not execute command", response.getByte(), equalTo(WorldErrorCodes.NOT_EXISTS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should available script languages")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS009() {
        ByteWrapper response = worldTestClient.scriptLanguageList();
        assertThat("Server should return script languages", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> languageDataList = response.getList();
        assertThat("Server should return at least 1 language", languageDataList.size() > 0, equalTo(true));

        List<String> languageList = languageDataList.stream()
            .map(ByteWrapper::getString)
            .collect(Collectors.toList());

        assertThat("Server should support ECMAScript", languageList.contains("ECMAScript"), equalTo(true));
    }


    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should not return script list to normal user")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS010() {
        dbHelper.cleanUpTable(Script.class, "");

        ByteWrapper response = worldTestClient.scriptList();
        assertThat("Server should not return script list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should return script list to admin")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS011() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws011", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptList();
        assertThat("Server should return script list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> scriptList = response.getList();
        assertThat("Script list should contain 1 element", scriptList.size(), equalTo(1));

        ByteWrapper scriptData = scriptList.get(0);
        assertThat("Script ID mismatch", scriptData.getInt(), equalTo(script.id));
        assertThat("Script name mismatch", scriptData.getString(), equalTo(script.name));
        assertThat("Script language mismatch", scriptData.getString(), equalTo(script.language));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should return script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS012() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws012", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptGet(script.id);
        assertThat("Server should return script", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        assertThat("Script ID mismatch", response.getInt(), equalTo(script.id));
        assertThat("Script name mismatch", response.getString(), equalTo(script.name));
        assertThat("Script language mismatch", response.getString(), equalTo(script.language));
        assertThat("Script content mismatch", response.getString(), equalTo(script.script));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should validate script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS013() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws013", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptValidate(script.language, script.script);
        assertThat("Server should validate script", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should invalidate script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS014() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws014", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java..type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptValidate(script.language, script.script);
        assertThat("Server should validate script", response.getByte(), equalTo(WorldErrorCodes.INVALID_SCRIPT));

        assertThat("Error line mismatch", response.getInt(), equalTo(2));
        assertThat("Error column mismatch", response.getInt(), equalTo(21));
        assertThat("Error message mismatch", response.getString().contains("Expected ident but found ."), equalTo(true));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should save script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS015() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws015", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptEdit(script.id, script.language, script.script.replaceAll("args", "arguments"));
        assertThat("Server should save script", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SCRIPT"}, description = "World Server should not save invalid script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS016() {
        dbHelper.cleanUpTable(Script.class, "");

        Script script = dbHelper.createScript("icws016", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Command = Java.extend(Base, {\n" +
            "  execute: function (dataSourceManager, session, args) {\n" +
            "    return new ByteArray().put(args);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Command();"
        );

        ByteWrapper response = worldTestClient.scriptEdit(
            script.id, script.language, script.script.replaceAll("Java\\.type", "Java..type")
        );
        assertThat("Server should not validate script", response.getByte(), equalTo(WorldErrorCodes.INVALID_SCRIPT));

        assertThat("Error line mismatch", response.getInt(), equalTo(1));
        assertThat("Error column mismatch", response.getInt(), equalTo(16));
        assertThat("Error message mismatch", response.getString().contains("Expected ident but found ."), equalTo(true));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHARACTER"}, description = "World Server should return location and character info")
    @Prerequisites(requires = { "session", "character" })
    public void testCaseICWS017() {
        ByteWrapper response = worldTestClient.authorize(session.getKey());
        assertThat("World Server should authorize session", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        int location = response.getInt();
        assertThat("Character location ID mismatch", location, equalTo(character.location));

        ByteWrapper characterInfo = response.getWrapper();

        int id = characterInfo.getInt();
        int realmId = characterInfo.getInt();
        int locationId = characterInfo.getInt();

        String firstName = characterInfo.getString();
        String lastName = characterInfo.getString();

        int raceId = characterInfo.getInt();
        String gender = characterInfo.getString();
        int classId = characterInfo.getInt();

        int level = characterInfo.getInt();
        long exp = characterInfo.getLong();
        long currency = characterInfo.getLong();

        byte[] body = characterInfo.getBytes();

        float positionX = characterInfo.getFloat();
        float positionY = characterInfo.getFloat();
        float positionZ = characterInfo.getFloat();

        assertThat("Character ID mismatch", id, equalTo(character.id));
        assertThat("Character realm ID mismatch", realmId, equalTo(character.realm.id));
        assertThat("Character location ID mismatch", locationId, equalTo(character.location));

        assertThat("Character first name mismatch", firstName, equalTo(character.firstName));
        assertThat("Character last name mismatch", lastName, equalTo(character.lastName));

        assertThat("Character race ID mismatch", raceId, equalTo(character.raceInfo.id));
        assertThat("Character gender mismatch", gender, equalTo(character.gender.toString().toLowerCase()));
        assertThat("Character class ID mismatch", classId, equalTo(character.classInfo.id));

        assertThat("Character level mismatch", level, equalTo(character.level));
        assertThat("Character exp mismatch", exp, equalTo(character.exp));
        assertThat("Character currency mismatch", currency, equalTo(character.currency));

        assertThat("Character position X mismatch", positionX, equalTo(character.positionX));
        assertThat("Character position Y mismatch", positionY, equalTo(character.positionY));
        assertThat("Character position Z mismatch", positionZ, equalTo(character.positionZ));

        assertThat("Character body data mismatch", body, equalTo(body));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHARACTER"}, description = "World Server should accept legal move")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS018() {
        float newX = character.positionX + WorldSize.MAX_SPEED;
        float newY = character.positionY;
        float newZ = character.positionZ;
        float newOrientation = character.orientation + 10f;

        ByteWrapper response = worldTestClient.move(newX, newY, newZ, newOrientation);

        assertThat("World Server should accept legal move", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        ByteWrapper position = response.getWrapper();

        assertThat("World Server should sent updated position X", position.getFloat(), equalTo(newX));
        assertThat("World Server should sent updated position Y", position.getFloat(), equalTo(newY));
        assertThat("World Server should sent updated position Z", position.getFloat(), equalTo(newZ));
        assertThat("World Server should sent updated position orientation", position.getFloat(), equalTo(newOrientation));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHARACTER"}, description = "World Server should not accept illegal move")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS019() {
        float newX = character.positionX + WorldSize.MAX_SPEED;
        float newY = character.positionY + WorldSize.MAX_SPEED;
        float newZ = character.positionZ;
        float newOrientation = character.orientation + 10f;

        ByteWrapper response = worldTestClient.move(newX, newY, newZ, newOrientation);

        assertThat("World Server should not accept illegal move", response.getByte(), equalTo(WorldErrorCodes.ILLEGAL_MOVE));

        ByteWrapper position = response.getWrapper();

        assertThat("World Server should sent old position X", position.getFloat(), equalTo(character.positionX));
        assertThat("World Server should sent old position Y", position.getFloat(), equalTo(character.positionY));
        assertThat("World Server should sent old position Z", position.getFloat(), equalTo(character.positionZ));
        assertThat("World Server should sent old position orientation", position.getFloat(), equalTo(character.orientation));
    }


    @Test(groups = {"IC", "ICWS", "ICWS_CHARACTER"}, description = "World Server should not accept move through obstacle")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS020() {
        final float step = (float) (WorldSize.MAX_SPEED / Math.sqrt(2.0)) * 0.9f;

        float newX = character.positionX + step;
        float newY = character.positionY - step;
        float newZ = character.positionZ;
        float newOrientation = character.orientation + 10f;

        ByteWrapper response = worldTestClient.move(newX, newY, newZ, newOrientation);

        assertThat("World Server should not accept illegal move", response.getByte(), equalTo(WorldErrorCodes.ILLEGAL_MOVE));

        ByteWrapper position = response.getWrapper();

        assertThat("World Server should sent old position X", position.getFloat(), equalTo(character.positionX));
        assertThat("World Server should sent old position Y", position.getFloat(), equalTo(character.positionY));
        assertThat("World Server should sent old position Z", position.getFloat(), equalTo(character.positionZ));
        assertThat("World Server should sent old position orientation", position.getFloat(), equalTo(character.orientation));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHARACTER"}, description = "World Server should send events of interest area changes")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS021() {
        final float centerX = character.positionX;
        final float centerY = character.positionY;

        final float step = (float) (WorldSize.MAX_SPEED / Math.sqrt(2.0)) * 0.9f;

        final float bottomLeftIAX = Math.max(centerX - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE);
        final float bottomLeftIAY = Math.max(centerY - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE);

        final float topRightIAX = Math.min(centerX + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE);
        final float topRightIAY = Math.min(centerY + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE);

        final float bottomLeftX = (int) Math.floor(bottomLeftIAX / WorldSize.CELL_SIZE) * WorldSize.CELL_SIZE;
        final float bottomLeftY = (int) Math.floor(bottomLeftIAY / WorldSize.CELL_SIZE) * WorldSize.CELL_SIZE;

        final float topRightX = ((int) Math.floor(topRightIAX / WorldSize.CELL_SIZE) + 1) * WorldSize.CELL_SIZE;
        final float topRightY = ((int) Math.floor(topRightIAY / WorldSize.CELL_SIZE) + 1) * WorldSize.CELL_SIZE;

        float currentX = bottomLeftX - step;
        float currentY = bottomLeftY - step;

        dbHelper.setCharacterPosition(character2, currentX, currentY, character2.positionZ, character2.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        currentX += step;
        currentY += step;

        ByteWrapper moveResponse = worldTestClient2.move(currentX, currentY, character2.positionZ, character2.orientation);
        assertThat("World Server should accept legal move to 2nd account", moveResponse.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        while ((currentX <= topRightX - step) && (currentY <= topRightY - step)) {
            currentX += step;
            currentY += step;

            moveResponse = worldTestClient2.move(currentX, currentY, character2.positionZ, character2.orientation);
            assertThat("World Server should accept legal move to 2nd account", moveResponse.getByte(), equalTo(CommonErrorCodes.SUCCESS));

            WorldEvent moveEvent = worldTestClient.waitForEvent(1, 100);
            assertThat("World Server should send MOVE event", moveEvent.getEventType(), equalTo(WorldEventType.MOVE));
            assertThat("ObjectID mismatch", moveEvent.getObjectId(), equalTo(objectId));

            ByteWrapper movementData = moveEvent.getEventData().getWrapper();
            assertThat("World Server should send valid player position X", movementData.getFloat(), equalTo(currentX));
            assertThat("World Server should send valid player position Y", movementData.getFloat(), equalTo(currentY));
            assertThat("World Server should send valid player position X", movementData.getFloat(), equalTo(character2.positionZ));
            assertThat("World Server should send valid player position orientation", movementData.getFloat(), equalTo(character2.orientation));
        }

        currentX += step;
        currentY += step;

        moveResponse = worldTestClient2.move(currentX, currentY, character2.positionZ, character2.orientation);
        assertThat("World Server should accept legal move to 2nd account", moveResponse.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent leaveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send LEAVE event", leaveEvent.getEventType(), equalTo(WorldEventType.LEAVE));
        assertThat("ObjectID mismatch", leaveEvent.getObjectId(), equalTo(objectId));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should return spell list")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS022() {
        Script script = dbHelper.createScript("icws022", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );

        Spell spell = dbHelper.createSpell("icws022", SpellType.SINGLE_TARGET, 0, character.classInfo, 1000L, 10f, 0f, 1, script);

        ByteWrapper response = worldTestClient.spellList();
        assertThat("World Server should return spell list", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        List<ByteWrapper> spellList = response.getList();
        assertThat("World Server should return 1 spell", spellList.size(), equalTo(1));

        ByteWrapper spellData = spellList.get(0);
        assertThat("Spell ID mismatch", spellData.getInt(), equalTo(spell.id));
        assertThat("Spell name mismatch", spellData.getString(), equalTo(spell.name));
        assertThat("Spell type mismatch", spellData.getString(), equalTo(spell.type.toString().toLowerCase()));
        assertThat("Spell distance mismatch", spellData.getFloat(), equalTo(spell.distance));
        assertThat("Spell radius mismatch", spellData.getFloat(), equalTo(spell.radius));
        assertThat("Spell basic potential mismatch", spellData.getLong(), equalTo(spell.basicPotential));
        assertThat("Spell cool down mismatch", spellData.getLong(), equalTo(spell.coolDown));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should cast single target spell")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS023() {
        dbHelper.setCharacterPosition(character2, character.positionX + 1f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws023", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws023", SpellType.SINGLE_TARGET, 0, character.classInfo, 1000L, 10f, 0f, 1, script);

        ByteWrapper spellCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should cast single target spell", spellCast.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent hitPointChangeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send HP_CHANGE event", hitPointChangeEvent.getEventType(), equalTo(WorldEventType.HP_CHANGE));
        assertThat("ObjectID mismatch", hitPointChangeEvent.getObjectId(), equalTo(objectId));

        hitPointChangeEvent.getObjectData().skip(4);

        long currentHitPoint = hitPointChangeEvent.getObjectData().getLong();
        long maxHitPoint = hitPointChangeEvent.getObjectData().getLong();

        assertThat("Spell should deal damage to 2nd character", maxHitPoint - currentHitPoint, equalTo(spell.basicPotential));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should not cast single target spell if target is out of range")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS024() {
        dbHelper.setCharacterPosition(character2, character.positionX + 10f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws024", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws024", SpellType.SINGLE_TARGET, 0, character.classInfo, 1000L, 5f, 0f, 1, script);

        ByteWrapper spellCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should cast single target spell", spellCast.getByte(), equalTo(WorldErrorCodes.OUT_OF_RANGE));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should cast area of effect spell")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS025() {
        dbHelper.setCharacterPosition(character2, character.positionX + 1f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws025", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws025", SpellType.AREA_OF_EFFECT, 0, character.classInfo, 1000L, 10f, 0f, 1, script);

        ByteWrapper spellCast = worldTestClient.spellCast(spell.id, character2.positionX, character2.positionY, character2.positionZ);
        assertThat("World Server should cast area of effect spell", spellCast.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent hitPointChangeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send HP_CHANGE event", hitPointChangeEvent.getEventType(), equalTo(WorldEventType.HP_CHANGE));
        assertThat("ObjectID mismatch", hitPointChangeEvent.getObjectId(), equalTo(objectId));

        hitPointChangeEvent.getObjectData().skip(4);

        long currentHitPoint = hitPointChangeEvent.getObjectData().getLong();
        long maxHitPoint = hitPointChangeEvent.getObjectData().getLong();

        assertThat("Spell should deal damage to 2nd character", maxHitPoint - currentHitPoint, equalTo(spell.basicPotential));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should not cast area of effect spell if target is out of range")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS026() {
        dbHelper.setCharacterPosition(character2, character.positionX + 10f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws026", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws026", SpellType.AREA_OF_EFFECT, 0, character.classInfo, 1000L, 5f, 0f, 1, script);

        ByteWrapper spellCast = worldTestClient.spellCast(spell.id, character2.positionX, character2.positionY, character2.positionZ);
        assertThat("World Server should cast area of effect spell", spellCast.getByte(), equalTo(WorldErrorCodes.OUT_OF_RANGE));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should not cast spell on cooldown")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS027() {
        dbHelper.setCharacterPosition(character2, character.positionX + 1f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws027", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws027", SpellType.SINGLE_TARGET, 0, character.classInfo, 5000L, 5f, 0f, 1, script);

        ByteWrapper spellFirstCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should cast single target spell", spellFirstCast.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent hitPointChangeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send HP_CHANGE event", hitPointChangeEvent.getEventType(), equalTo(WorldEventType.HP_CHANGE));
        assertThat("ObjectID mismatch", hitPointChangeEvent.getObjectId(), equalTo(objectId));

        hitPointChangeEvent.getObjectData().skip(4);

        long currentHitPoint = hitPointChangeEvent.getObjectData().getLong();
        long maxHitPoint = hitPointChangeEvent.getObjectData().getLong();

        assertThat("Spell should deal damage to 2nd character", maxHitPoint - currentHitPoint, equalTo(spell.basicPotential));

        ByteWrapper spellSecondCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should not cast spell on cooldown", spellSecondCast.getByte(), equalTo(WorldErrorCodes.COOLDOWN));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_SPELL"}, description = "World Server should cast spell after cooldown")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS028() throws InterruptedException {
        dbHelper.setCharacterPosition(character2, character.positionX + 1f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        Script script = dbHelper.createScript("icws028", "ECMAScript",
            "var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');\n" +
            "var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');\n" +
            "\n" +
            "var Spell = Java.extend(Base, {\n" +
            "  cast: function (caster, target, potential) {\n" +
            "    target.processHitPointChange(-potential);\n" +
            "  }\n" +
            "});\n" +
            "\n" +
            "var sObject = new Spell();"
        );
        Spell spell = dbHelper.createSpell("icws028", SpellType.SINGLE_TARGET, 0, character.classInfo, 5000L, 5f, 0f, 1, script);

        ByteWrapper spellFirstCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should cast single target spell", spellFirstCast.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent hitPointChangeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send HP_CHANGE event", hitPointChangeEvent.getEventType(), equalTo(WorldEventType.HP_CHANGE));
        assertThat("ObjectID mismatch", hitPointChangeEvent.getObjectId(), equalTo(objectId));

        hitPointChangeEvent.getObjectData().skip(4);

        long currentHitPoint = hitPointChangeEvent.getObjectData().getLong();
        long maxHitPoint = hitPointChangeEvent.getObjectData().getLong();

        assertThat("Spell should deal damage to 2nd character", maxHitPoint - currentHitPoint, equalTo(spell.basicPotential));

        ByteWrapper spellSecondCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should not cast spell on cooldown", spellSecondCast.getByte(), equalTo(WorldErrorCodes.COOLDOWN));
        long cooldown = spellSecondCast.getLong();

        Thread.sleep(cooldown);

        ByteWrapper spellThirdCast = worldTestClient.spellCast(spell.id, objectId);
        assertThat("World Server should cast single target spell", spellThirdCast.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        hitPointChangeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send HP_CHANGE event", hitPointChangeEvent.getEventType(), equalTo(WorldEventType.HP_CHANGE));
        assertThat("ObjectID mismatch", hitPointChangeEvent.getObjectId(), equalTo(objectId));

        hitPointChangeEvent.getObjectData().skip(4);

        currentHitPoint = hitPointChangeEvent.getObjectData().getLong();
        maxHitPoint = hitPointChangeEvent.getObjectData().getLong();

        assertThat("Spell should deal damage to 2nd character", maxHitPoint - currentHitPoint, equalTo(spell.basicPotential * 2));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHAT"}, description = "World Server should deliver local message")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS029() {
        dbHelper.setCharacterPosition(character2, character.positionX + 10f, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        final String message = character.lastName;

        WorldEvent subscribeEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send SUBSCRIBE event", subscribeEvent.getEventType(), equalTo(WorldEventType.SUBSCRIBE));
        assertThat("Object name mismatch", subscribeEvent.getObjectName(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        OID objectId = subscribeEvent.getObjectId();

        WorldEvent enterEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send ENTER event", enterEvent.getEventType(), equalTo(WorldEventType.ENTER));
        assertThat("ObjectID mismatch", enterEvent.getObjectId(), equalTo(objectId));

        WorldEvent firstMoveEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send MOVE event", firstMoveEvent.getEventType(), equalTo(WorldEventType.MOVE));
        assertThat("ObjectID mismatch", firstMoveEvent.getObjectId(), equalTo(objectId));

        ByteWrapper messageSend = worldTestClient2.sendMessage(ChatMessageType.LOCAL, null, message);
        assertThat("World Server should send local message", messageSend.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent chatMessageEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send CHAT_MESSAGE event", chatMessageEvent.getEventType(), equalTo(WorldEventType.CHAT_MESSAGE));
        assertThat("ObjectID mismatch", chatMessageEvent.getObjectId(), equalTo(objectId));

        assertThat("Message source ID mismatch", chatMessageEvent.getEventData().getOID(), equalTo(objectId));
        assertThat("Message source name mismatch", chatMessageEvent.getEventData().getString(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        assertThat("Message text mismatch", chatMessageEvent.getEventData().getString(), equalTo(message));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHAT"}, description = "World Server should deliver broadcast message")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS030() {
        dbHelper.setCharacterPosition(character2, character.positionX + WorldSize.OUTER_INTEREST_AREA_RADIUS * 2, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        final String message = character.lastName;

        ByteWrapper messageSend = worldTestClient2.sendMessage(ChatMessageType.BROADCAST, null, message);
        assertThat("World Server should send broadcast message", messageSend.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent chatMessageEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send CHAT_MESSAGE event", chatMessageEvent.getEventType(), equalTo(WorldEventType.CHAT_MESSAGE));
        OID objectId = chatMessageEvent.getObjectId();

        assertThat("Message source ID mismatch", chatMessageEvent.getEventData().getOID(), equalTo(objectId));
        assertThat("Message source name mismatch", chatMessageEvent.getEventData().getString(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        assertThat("Message text mismatch", chatMessageEvent.getEventData().getString(), equalTo(message));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHAT"}, description = "World Server should deliver private message")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS031() {
        dbHelper.setCharacterPosition(character2, character.positionX + WorldSize.OUTER_INTEREST_AREA_RADIUS * 2, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        final String message = character.lastName;

        ByteWrapper messageSend = worldTestClient2.sendMessage(ChatMessageType.PRIVATE, String.format("%s %s", character.firstName, character.lastName), message);
        assertThat("World Server should send private message", messageSend.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent chatMessageEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send CHAT_MESSAGE event", chatMessageEvent.getEventType(), equalTo(WorldEventType.CHAT_MESSAGE));
        OID objectId = chatMessageEvent.getObjectId();

        assertThat("Message source ID mismatch", chatMessageEvent.getEventData().getOID(), equalTo(objectId));
        assertThat("Message source name mismatch", chatMessageEvent.getEventData().getString(), equalTo(String.format("%s %s", character2.firstName, character2.lastName)));
        assertThat("Message text mismatch", chatMessageEvent.getEventData().getString(), equalTo(message));
    }


    @Test(groups = {"IC", "ICWS", "ICWS_CHAT"}, description = "World Server should not deliver private message to offline user")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player" })
    public void testCaseICWS032() {
        final String message = character.lastName;

        ByteWrapper messageSend = worldTestClient.sendMessage(ChatMessageType.PRIVATE, String.format("%s %s", character2.firstName, character2.lastName), message);
        assertThat("World Server should not send private message", messageSend.getByte(), equalTo(WorldErrorCodes.NOT_EXISTS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS_CHAT"}, description = "World Server should deliver announce from admin")
    @Prerequisites(requires = { "session", "character", "auth", "2nd_player", "admin" })
    public void testCaseICWS033() {
        dbHelper.setCharacterPosition(character2, character.positionX + WorldSize.OUTER_INTEREST_AREA_RADIUS * 2, character.positionY, character.positionZ, character.orientation);
        dbHelper.selectCharacter(session2, character2);

        ByteWrapper response = worldTestClient2.authorize(session2.getKey());
        assertThat("World Server should authorize session for 2nd account", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        final String message = character.lastName;

        ByteWrapper messageSend = worldTestClient2.sendMessage(ChatMessageType.ANNOUNCE, null, message);
        assertThat("World Server should send announce", messageSend.getByte(), equalTo(CommonErrorCodes.SUCCESS));

        WorldEvent chatMessageEvent = worldTestClient.waitForEvent(10, 100);
        assertThat("World Server should send CHAT_MESSAGE event", chatMessageEvent.getEventType(), equalTo(WorldEventType.CHAT_MESSAGE));
        OID objectId = chatMessageEvent.getObjectId();

        assertThat("Message source ID mismatch", chatMessageEvent.getEventData().getOID(), equalTo(objectId));
        assertThat("Message source name mismatch", chatMessageEvent.getEventData().getString(), equalTo("World"));
        assertThat("Message text mismatch", chatMessageEvent.getEventData().getString(), equalTo(message));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        account = null;
        account2 = null;

        session = null;
        session2 = null;

        character = null;
        character2 = null;

        worldTestClient = null;
        worldTestClient2 = null;

        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
            testClient = null;
        }

        if ((testClient2 != null)&&testClient2.isConnected()) {
            testClient2.disconnect();
            testClient2 = null;
        }
    }
}
