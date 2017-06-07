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
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.AbstractIT;
import ru.infernoproject.tests.annotations.Prerequisites;
import ru.infernoproject.tests.client.TestClient;
import ru.infernoproject.worldd.constants.WorldErrorCodes;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class WorldServerTest extends AbstractIT {

    private WorldTestClient worldTestClient;

    private Account account;
    private Session session;

    @BeforeClass(alwaysRun = true)
    public void cleanUpDataBase() {
        dbHelper.cleanUpTable(Account.class);
        dbHelper.cleanUpTable(Session.class);

        dbHelper.cleanUpTable(RealmListEntry.class);

        dbHelper.cleanUpTable(RaceInfo.class);
        dbHelper.cleanUpTable(ClassInfo.class);

        dbHelper.cleanUpTable(CharacterInfo.class);

        dbHelper.cleanUpTable(Command.class);
        dbHelper.cleanUpTable(Script.class);
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpTestClient(Method testMethod) {
        testClient = getTestClient("world");
        assertThat("Unable to connect to server", testClient.isConnected(), equalTo(true));

        worldTestClient = new WorldTestClient(testClient);

        if (testMethod.isAnnotationPresent(Prerequisites.class)) {
            Prerequisites prerequisites = testMethod.getAnnotation(Prerequisites.class);
            List<String> requirements = Arrays.asList(prerequisites.requires());

            if (requirements.contains("session")) {
                account = dbHelper.createUser(testMethod.getName(), "testPassword");
                session = dbHelper.createSession(account, testClient.getAddress());
            }

            if (requirements.contains("admin")) {
                if (!requirements.contains("session"))
                    throw new RuntimeException("Session required");
                dbHelper.setUserAccessLevel(account, AccountLevel.ADMIN);
            }

            if (requirements.contains("character")) {
                if (!requirements.contains("session"))
                    throw new RuntimeException("Session required for character creation");

                RaceInfo raceInfo = dbHelper.createRace(testMethod.getName(), testMethod.getName());
                ClassInfo classInfo = dbHelper.createClass(testMethod.getName(), testMethod.getName());

                RealmListEntry realmListEntry = dbHelper.createRealmIfNotExists("Test World", "test_world", 8085);
                CharacterInfo characterInfo = dbHelper.createCharacter(account, realmListEntry, "testCharacter", testMethod.getName(), GenderInfo.FEMALE, raceInfo, classInfo, new byte[0]);

                dbHelper.selectCharacter(session, characterInfo);
            }

            if (requirements.contains("auth")) {
                if (!requirements.contains("character"))
                    throw new RuntimeException("Character required for authentication");

                ByteWrapper response = worldTestClient.authorize(session.getKey());
                assertThat("World Server should authorize session", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
            }
        }
    }

    @Test(groups = {"IC", "ICWS", "ICWS001"}, description = "World Server should authorize session")
    @Prerequisites(requires = { "session", "character" })
    public void testCaseICWS001() {
        ByteWrapper response = worldTestClient.authorize(session.getKey());
        assertThat("World Server should authorize session", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS002"}, description = "World Server should not authorize invalid session")
    public void testCaseICWS002() {
        byte[] invalidSession = new byte[16];
        new Random().nextBytes(invalidSession);

        ByteWrapper response = worldTestClient.authorize(invalidSession);
        assertThat("World Server not should authorize invalid session", response.getByte(), equalTo(CommonErrorCodes.AUTH_ERROR));
    }

    @Test(groups = {"IC", "ICWS", "ICWS003"}, description = "World Server should allow to log out")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS003() {
        ByteWrapper response = worldTestClient.logOut();
        assertThat("World Server should allow to log out", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS004"}, description = "World Server should not allow to log out for unauthorized users")
    public void testCaseICWS004() {
        ByteWrapper response = worldTestClient.logOut();
        assertThat("World Server should not allow to log out", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = {"IC", "ICWS", "ICWS005"}, description = "World Server should respond to heartbeat")
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

    @Test(groups = {"IC", "ICWS", "ICWS006"}, description = "World Server should execute command")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS006() {
        Command command = dbHelper.createCommand("icws006", AccountLevel.USER,
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

    @Test(groups = {"IC", "ICWS", "ICWS007"}, description = "World Server should not execute command if user has not enough access level")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS007() {
        Command command = dbHelper.createCommand("icws007", AccountLevel.ADMIN,
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

    @Test(groups = {"IC", "ICWS", "ICWS008"}, description = "World Server should not execute nonexistent")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS008() {
        ByteWrapper response = worldTestClient.executeCommand("nonexistent", "arg1", "arg2");
        assertThat("World Server should not execute command", response.getByte(), equalTo(WorldErrorCodes.NOT_EXISTS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS009"}, description = "World Server should not return script list to normal user")
    @Prerequisites(requires = { "session", "character", "auth" })
    public void testCaseICWS009() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws009",
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
        assertThat("Server should not return script list", response.getByte(), equalTo(CommonErrorCodes.AUTH_REQUIRED));
    }

    @Test(groups = {"IC", "ICWS", "ICWS010"}, description = "World Server should return script list to admin")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS010() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws009",
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
    }

    @Test(groups = {"IC", "ICWS", "ICWS011"}, description = "World Server should return script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS011() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws011",
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
        assertThat("Script content mismatch", response.getString(), equalTo(script.script));
    }

    @Test(groups = {"IC", "ICWS", "ICWS012"}, description = "World Server should validate script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS012() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws012",
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

        ByteWrapper response = worldTestClient.scriptValidate(script.script);
        assertThat("Server should validate script", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS013"}, description = "World Server should invalidate script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS013() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws013",
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

        ByteWrapper response = worldTestClient.scriptValidate(script.script);
        assertThat("Server should validate script", response.getByte(), equalTo(WorldErrorCodes.INVALID_SCRIPT));

        assertThat("Error line mismatch", response.getInt(), equalTo(2));
        assertThat("Error column mismatch", response.getInt(), equalTo(21));
        assertThat("Error message mismatch", response.getString().contains("Expected ident but found ."), equalTo(true));
    }

    @Test(groups = {"IC", "ICWS", "ICWS014"}, description = "World Server should save script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS014() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws014",
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

        ByteWrapper response = worldTestClient.scriptEdit(script.id, script.script.replaceAll("args", "arguments"));
        assertThat("Server should save script", response.getByte(), equalTo(CommonErrorCodes.SUCCESS));
    }

    @Test(groups = {"IC", "ICWS", "ICWS015"}, description = "World Server should not save invalid script")
    @Prerequisites(requires = { "session", "character", "auth", "admin" })
    public void testCaseICWS015() {
        dbHelper.cleanUpTable(Script.class);

        Script script = dbHelper.createScript("icws015",
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
            script.id, script.script.replaceAll("Java\\.type", "Java..type")
        );
        assertThat("Server should validate script", response.getByte(), equalTo(WorldErrorCodes.INVALID_SCRIPT));

        assertThat("Error line mismatch", response.getInt(), equalTo(1));
        assertThat("Error column mismatch", response.getInt(), equalTo(16));
        assertThat("Error message mismatch", response.getString().contains("Expected ident but found ."), equalTo(true));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownTestClient() {
        if ((testClient != null)&&testClient.isConnected()) {
            testClient.disconnect();
        }
    }
}
