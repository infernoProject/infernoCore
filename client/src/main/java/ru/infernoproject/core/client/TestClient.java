package ru.infernoproject.core.client;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.client.realm.RealmClient;
import ru.infernoproject.core.client.world.WorldClient;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.common.utils.Result;

import java.util.List;

import static ru.infernoproject.core.common.constants.WorldEventType.*;

public class TestClient {

    private RealmClient realmClient;
    private WorldClient worldClient;

    private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

    private TestClient(String host, int port) {
        realmClient = new RealmClient(host, port);

        try {
            realmClient.srp6ConfigGet();
        } catch (InterruptedException e) {
            error("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void error(String message) {
        logger.error(message);
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
            if (!realmList.isEmpty()) {
                RealmServerInfo realmServer = realmList.get(0);

                logger.info(realmServer.toString());

                realmClient.serverSelect(realmServer);

                worldClient = realmClient.serverConnect();
                worldClient.setEventListener(this::onEvent);

                realmClient.sessionTokenGet(this::token);
            } else {
                error("Empty RealmList");
            }
        } else {
            error(result.attr("message"));
        }
    }

    private void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax) {
        logger.info(String.format("Health: %5.2f%%", (float) healthCurrent / (float) healthMax * 100));

        switch (type) {
            case DAMAGE:
                logger.info("Got damage: {}", quantifier);
                break;
            case HEAL:
                logger.info("Got healing: {}", quantifier);
                break;
            case DEATH:
                logger.info("Killed");
                break;
            case REVIVE:
                logger.info("Revived");
                break;
            case AURA:
                logger.info("Buffed for Aura({}): {}ms", quantifier, duration);
                break;
            case UN_AURA:
                logger.info("Aura({}) expired", quantifier);
                break;
            default:
                logger.info(String.format("Event(%02X): %d : %d", type, quantifier, duration));
                break;
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
            result.attr(List.class, "characters")
                .forEach(character -> logger.info(character.toString()));

            worldClient.characterSelect(
                (CharacterInfo) result.attr(List.class, "characters").get(0),
                this::characterSelect
            );
        } else {
            error(result.attr("message"));
        }
    }

    private void characterSelect(Result result) {
        if (result.isSuccess()) {
            worldClient.commandExecute("learn", new String[] {"1", "2", "3", "4"}, this::commandExec);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(4, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(1, this::spellCast);
            worldClient.spellCast(3, this::spellCast);
            worldClient.spellCast(2, this::spellCast);
            worldClient.spellCast(4, this::spellCast);
        } else {
            error(result.attr("message"));
        }
    }

    @SuppressWarnings("unchecked")
    private void commandExec(Result result) {
        String[] output = result.attr(String[].class,"output");

        if (result.isSuccess()) {
            for (String line: output)
                logger.info("OUT: {}", line);
        } else {
            for (String line: output)
                logger.info("ERR: {}", line);
        }
    }

    private void spellCast(Result result) {
        if (result.isSuccess()) {
            logger.info("Cast successful");
        } else {
            logger.info("Unable to cast: {}", result.attr("message"));
        }
    }

    private void disconnect() {
        worldClient.disconnect();
    }

    public static void main(String[] args) {
        if (Boolean.getBoolean(System.getProperty("debug", "false"))) {
            LogManager.getRootLogger().setLevel(Level.DEBUG);
        }

        TestClient testClient = new TestClient(
            System.getProperty("serverHost", "0.0.0.0"),
            Integer.parseInt(System.getProperty("serverPort", "3274"))
        );

        testClient.logIn(
            System.getProperty("userLogin"),
            System.getProperty("userPassword")
        );

        try {
            Thread.sleep(17000);
        } catch (InterruptedException e) {
            testClient.error("Interrupted");
            Thread.currentThread().interrupt();
        }

        testClient.disconnect();
    }
}
