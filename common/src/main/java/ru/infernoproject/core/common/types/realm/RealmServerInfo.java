package ru.infernoproject.core.common.types.realm;

import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;

import java.nio.ByteBuffer;

public class RealmServerInfo implements ByteConvertible {

    private final String name;
    private final int type;
    private final String serverHost;
    private final int serverPort;

    public RealmServerInfo(String name, int type, String serverHost, int serverPort) {
        this.name = name;
        this.type = type;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public RealmServerInfo(ByteWrapper serverData) {
        name = serverData.getString();
        type = serverData.getInt();
        serverHost = serverData.getString();
        serverPort = serverData.getInt();
    }

    @Override
    public byte[] toByteArray() {
        byte[] nameBytes = name.getBytes();
        byte[] hostBytes = serverHost.getBytes();

        ByteBuffer byteBuffer = ByteBuffer.allocate(16 + nameBytes.length + hostBytes.length);

        byteBuffer.putInt(nameBytes.length);
        byteBuffer.put(nameBytes);

        byteBuffer.putInt(type);

        byteBuffer.putInt(hostBytes.length);
        byteBuffer.put(hostBytes);

        byteBuffer.putInt(serverPort);

        return byteBuffer.array();
    }

    @Override
    public String toString() {
        return String.format(
            "RealmServerInfo(name='%s', server=%s:%d, type=%d)",
            name, serverHost, serverPort, type
        );
    }

    public String getHost() {
        return serverHost;
    }

    public int getPort() {
        return serverPort;
    }
}
