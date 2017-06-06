package ru.infernoproject.tests.worldd;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.tests.client.TestClient;
import ru.infernoproject.worldd.constants.WorldOperations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class WorldTestClient {

    private final TestClient testClient;

    public WorldTestClient(TestClient testClient) {
        this.testClient = testClient;
    }

    public ByteWrapper authorize(byte[] session) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.AUTHORIZE).put(session));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.AUTHORIZE));

        return response.getWrapper();
    }

    public ByteWrapper logOut() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.LOG_OUT));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.LOG_OUT));

        return response.getWrapper();
    }

    public ByteWrapper heartBeat() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.HEART_BEAT).put(System.currentTimeMillis()));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.HEART_BEAT));

        return response.getWrapper();
    }

    public ByteWrapper executeCommand(String command, String... args) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.EXECUTE).put(command).put(args));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.EXECUTE));

        return response.getWrapper();
    }
}
