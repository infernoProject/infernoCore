package ru.infernoproject.tests.db;

import ru.infernoproject.common.auth.sql.Account;

import ru.infernoproject.common.auth.sql.AccountBan;
import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.tests.crypto.CryptoHelper;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DBHelper {

    private final DataSourceManager dataSourceManager;
    private final CryptoHelper cryptoHelper;

    public DBHelper(DataSourceManager dataSourceManager, CryptoHelper cryptoHelper) {
        this.dataSourceManager = dataSourceManager;
        this.cryptoHelper = cryptoHelper;
    }

    public <T extends SQLObjectWrapper> void cleanUpTable(Class<T> model) {
        try {
            dataSourceManager.query(model).delete("");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Account createUser(String login, String password) {
        try {
            byte[] clientSalt = cryptoHelper.generateSalt();
            byte[] clientVerifier = cryptoHelper.calculateVerifier(login, password, clientSalt);

            dataSourceManager.query(Account.class).insert(new Account(
                login, AccountLevel.USER, String.format("%s@testCase", login), clientSalt, clientVerifier
            ));

            return dataSourceManager.query(Account.class).select()
                .filter(new SQLFilter("login").eq(login))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUserAccessLevel(Account account, AccountLevel level) {
        try {
            account.accessLevel = level;

            dataSourceManager.query(Account.class).update(account);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Session createSession(Account account, SocketAddress address) {
        try {
            Session session = new Session(account, cryptoHelper.generateSalt(), address);

            dataSourceManager.query(Session.class).insert(session);

            return dataSourceManager.query(Session.class).select()
                .filter(new SQLFilter("session_key").eq(session.getKeyHex()))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RealmListEntry createRealmIfNotExists(String name, String host, int port) {
        try {
            RealmListEntry realmListEntry = dataSourceManager.query(RealmListEntry.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();

            if (realmListEntry != null)
                return realmListEntry;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return createRealm(name, host, port);
    }

    public RealmListEntry createRealm(String name, String host, int port) {
        try {
            RealmListEntry realmServer = new RealmListEntry();

            realmServer.name = name;
            realmServer.type = 1;
            realmServer.online = 1;
            realmServer.lastSeen = LocalDateTime.now();

            realmServer.serverHost = host;
            realmServer.serverPort = port;

            dataSourceManager.query(RealmListEntry.class).insert(realmServer);

            return dataSourceManager.query(RealmListEntry.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ClassInfo createClass(String name, String resource) {
        try {
            ClassInfo classInfo = new ClassInfo();

            classInfo.name = name;
            classInfo.resource = resource;

            dataSourceManager.query(ClassInfo.class).insert(classInfo);

            return dataSourceManager.query(ClassInfo.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RaceInfo createRace(String name, String resource) {
        try {
            RaceInfo raceInfo = new RaceInfo();

            raceInfo.name = name;
            raceInfo.resource = resource;

            dataSourceManager.query(RaceInfo.class).insert(raceInfo);

            return dataSourceManager.query(RaceInfo.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterInfo createCharacter(CharacterInfo characterInfo) {
        try {
            dataSourceManager.query(CharacterInfo.class).insert(characterInfo);

            return dataSourceManager.query(CharacterInfo.class).select()
                .filter(new SQLFilter().and(
                    new SQLFilter("realm").eq(characterInfo.realm.id),
                    new SQLFilter("first_name").eq(characterInfo.firstName),
                    new SQLFilter("last_name").eq(characterInfo.lastName),
                    new SQLFilter("delete_flag").eq(0)
                )).fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterInfo createCharacter(Account account, RealmListEntry realm, String firstName, String lastName, GenderInfo genderInfo, RaceInfo raceInfo, ClassInfo classInfo, byte[] body) {
        CharacterInfo characterInfo = new CharacterInfo();

        characterInfo.account = account;
        characterInfo.realm = realm;
        characterInfo.firstName = firstName;
        characterInfo.lastName = lastName;
        characterInfo.gender = genderInfo;
        characterInfo.raceInfo = raceInfo;
        characterInfo.classInfo = classInfo;
        characterInfo.body = body;

        return createCharacter(characterInfo);
    }

    public void deleteCharacter(CharacterInfo characterInfo) {
        deleteCharacter(characterInfo, false);
    }

    public void deleteCharacter(CharacterInfo characterInfo, boolean force) {
        try {
            if (force) {
                dataSourceManager.query(CharacterInfo.class).delete(characterInfo);
            } else {
                dataSourceManager.query(CharacterInfo.class).update(
                    "SET `delete_flag` = 1, `delete_after` = DATE_ADD(NOW(), INTERVAL 1 MINUTE) WHERE `id` = " + characterInfo.id
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void selectCharacter(Session session, CharacterInfo characterInfo) {
        try {
            session.characterInfo = characterInfo;

            dataSourceManager.query(Session.class).update(session);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Script createScript(String name, String script) {
        try {
            Script scriptData = new Script();

            scriptData.name = name;
            scriptData.script = script;

            dataSourceManager.query(Script.class).insert(scriptData);

            return dataSourceManager.query(Script.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Command createCommand(String name, AccountLevel accessLevel, String script) {
        try {
            Script scriptData = createScript(name, script);

            Command commandData = new Command();

            commandData.name = name;
            commandData.level = accessLevel;
            commandData.script = scriptData;

            dataSourceManager.query(Command.class).insert(commandData);

            return dataSourceManager.query(Command.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountBan banAccount(Account account, long expiresIn, String reason) {
        try {
            AccountBan ban = new AccountBan();

            ban.account = account;
            ban.expires = LocalDateTime.now().plusSeconds(expiresIn);
            ban.reason = reason;

            dataSourceManager.query(AccountBan.class).insert(ban);

            return dataSourceManager.query(AccountBan.class).select()
                .filter(new SQLFilter("account").eq(account.id))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
