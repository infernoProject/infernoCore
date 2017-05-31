package ru.infernoproject.tests.db;

import ru.infernoproject.common.auth.sql.Account;

import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.tests.crypto.CryptoHelper;

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

    public Account createUser(String login, String password) {
        try {
            byte[] clientSalt = cryptoHelper.generateSalt();
            byte[] clientVerifier = cryptoHelper.calculateVerifier(login, password, clientSalt);

            dataSourceManager.query(Account.class).insert(new Account(
                login, "user", String.format("%s@testCase", login), clientSalt, clientVerifier
            ));

            return dataSourceManager.query(Account.class).select()
                .filter(new SQLFilter("login").eq(login))
                .fetchOne();
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
}
