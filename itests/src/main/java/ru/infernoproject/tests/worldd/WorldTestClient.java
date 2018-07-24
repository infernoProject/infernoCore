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

    public WorldEvent waitForEvent(int retryCount, int timeOut) {
        try {
            ByteWrapper response = testClient.receive(retryCount, timeOut);
            assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.EVENT));

            return new WorldEvent(response.getWrapper());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    public ByteWrapper scriptList() {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.SCRIPT_LIST));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.SCRIPT_LIST));

        return response.getWrapper();
    }

    public ByteWrapper scriptGet(int id) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.SCRIPT_GET).put(id));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.SCRIPT_GET));

        return response.getWrapper();
    }

    public ByteWrapper scriptValidate(String script) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.SCRIPT_VALIDATE).put(script));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.SCRIPT_VALIDATE));

        return response.getWrapper();
    }

    public ByteWrapper scriptEdit(int id, String script) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.SCRIPT_SAVE).put(id).put(script));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.SCRIPT_SAVE));

        return response.getWrapper();
    }

    public ByteWrapper move(float x, float y, float z, float orientation) {
        ByteWrapper response = testClient.sendReceive(new ByteArray(WorldOperations.MOVE).put(x).put(y).put(z).put(orientation));
        assertThat("Invalid OPCode", response.getByte(), equalTo(WorldOperations.MOVE));

        return response.getWrapper();
    }
}
