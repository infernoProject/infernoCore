package ru.infernoproject.core.client;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ru.infernoproject.core.client.realm.RealmClient;
import ru.infernoproject.core.client.world.WorldClient;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.utils.Result;

import java.util.List;

public class TestClient {

    private RealmClient realmClient;
    private WorldClient worldClient;

    public TestClient(String host, int port) {
        realmClient = new RealmClient(host, port);

        try {
            realmClient.srp6ConfigGet();
        } catch (InterruptedException e) {
            error("Interrupted");
        }
    }

    private void error(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private void logIn(String login, String password) {
        realmClient.logIn(
            login, password,
            this::logInCallBack
        );
    }

    private void logInCallBack(Result result) {
        if (result.isSuccess()) {
            realmClient.realmListGet(this::setRealms);
        } else {
            error(result.attr("message"));
        }
    }

    @SuppressWarnings("unchecked")
    private void setRealms(Result result) {
        if (result.isSuccess()) {
            List<RealmServerInfo> realmList = (List<RealmServerInfo>) result.attr(List.class, "realmList");
            if (realmList.size() > 0) {
                RealmServerInfo realmServer = realmList.get(0);

                System.out.println(realmServer);

                realmClient.serverSelect(realmServer);

                worldClient = realmClient.serverConnect();
                realmClient.sessionTokenGet(this::token);
            } else {
                error("Empty RealmList");
            }
        } else {
            error(result.attr("message"));
        }
    }

    private void token(Result result) {
        if (result.isSuccess()) {
            worldClient.authorize(
                result.attr(byte[].class, "sessionToken"),
                this::authorize
            );
        } else {
            error(result.attr("message"));
        }
    }

    private void authorize(Result result) {
        if (result.isSuccess()) {
            realmClient.disconnect();

            worldClient.characterListGet(this::characterList);
        } else {
            error(result.attr("message"));
        }
    }

    @SuppressWarnings("unchecked")
    private void characterList(Result result) {
        if (result.isSuccess()) {
            result.attr(List.class, "characters").forEach(System.out::println);

            worldClient.disconnect();
        } else {
            worldClient.disconnect();
            error(result.attr("message"));
        }
    }

    public static void main(String[] args) {
        Logger.getRootLogger().setLevel(Level.OFF);

        TestClient testClient = new TestClient(
            System.getProperty("serverHost", "0.0.0.0"),
            Integer.parseInt(System.getProperty("serverPort", "3274"))
        );

        testClient.logIn(
            System.getProperty("userLogin"),
            System.getProperty("userPassword")
        );
    }
}
