package ru.infernoproject.tests.realmd;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.realmd.constants.RealmOperations;
import ru.infernoproject.tests.client.TestClient;
import ru.infernoproject.tests.crypto.CryptoHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RealmTestClient {
    
    private final TestClient testClient;
    private final CryptoHelper cryptoHelper;
    
    public RealmTestClient(TestClient testClient, CryptoHelper cryptoHelper) {
        this.testClient = testClient;
        this.cryptoHelper = cryptoHelper;
    }

    public ByteWrapper selectCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CHARACTER_SELECT).put(characterInfo.id));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_SELECT));
        return response.getWrapper();
    }

    public ByteWrapper restoreCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CHARACTER_RESTORE).put(characterInfo.id));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_RESTORE));
        return response.getWrapper();
    }

    public ByteWrapper getRestoreableCharacterList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CHARACTER_RESTOREABLE_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_RESTOREABLE_LIST));
        return response.getWrapper();
    }

    public ByteWrapper deleteCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CHARACTER_DELETE).put(characterInfo.id));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_DELETE));
        return response.getWrapper();
    }

    public ByteWrapper getCharacterList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CHARACTER_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_LIST));
        return response.getWrapper();
    }

    public ByteWrapper createCharacter(CharacterInfo characterInfo) {
        ByteWrapper response = testClient.sendReceive(
            new ByteArray(RealmOperations.CHARACTER_CREATE).put(characterInfo.realm.id)
                .put(characterInfo.firstName).put(characterInfo.lastName).put(characterInfo.gender.toString().toLowerCase())
                .put(characterInfo.raceInfo.id).put(characterInfo.classInfo.id).put(characterInfo.body)
        );

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CHARACTER_CREATE));
        return response.getWrapper();
    }

    public ByteWrapper getClassList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.CLASS_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.CLASS_LIST));
        return response.getWrapper();
    }

    public ByteWrapper getRaceList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.RACE_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.RACE_LIST));
        return response.getWrapper();
    }

    public ByteWrapper getRealmList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.REALM_LIST));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.REALM_LIST));
        return response.getWrapper();
    }

    public ByteWrapper getSessionToken() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.SESSION_TOKEN));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SESSION_TOKEN));
        return response.getWrapper();
    }

    public ByteWrapper logInStep1(String login) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.LOG_IN_STEP1).put(login));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP1));
        return response.getWrapper();
    }

    public ByteWrapper logInStep2(String login, String password, ByteWrapper loginChallenge) {
        byte[] sessionKey = loginChallenge.getBytes();

        byte[] clientSalt = loginChallenge.getBytes();
        byte[] vector = loginChallenge.getBytes();

        return logInStep2(login, password, sessionKey, vector, clientSalt);
    }

    public ByteWrapper logInStep2(String login, String password, byte[] sessionKey, byte[] vector, byte[] clientSalt) {
        byte[] challenge = cryptoHelper.calculateChallenge(login, password, vector, clientSalt);

        ByteWrapper response = testClient.sendReceive(new ByteArray(RealmOperations.LOG_IN_STEP2).put(sessionKey).put(challenge));

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.LOG_IN_STEP2));
        return response.getWrapper();
    }

    public ByteWrapper registerUser(String login, String password) {
        byte[] clientSalt = cryptoHelper.generateSalt();
        byte[] clientVerifier = cryptoHelper.calculateVerifier(login, password, clientSalt);

        ByteWrapper response = testClient.sendReceive(
            new ByteArray(RealmOperations.SIGN_UP)
                .put(login).put(String.format("%s@testCase", login))
                .put(clientSalt).put(clientVerifier)
        );

        assertThat("Invalid OPCode", response.getByte(), equalTo(RealmOperations.SIGN_UP));

        return response.getWrapper();
    }
}
