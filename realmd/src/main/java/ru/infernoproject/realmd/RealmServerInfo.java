package ru.infernoproject.realmd;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

import java.nio.ByteBuffer;

@SQLObject(table = "realm_list", database = "realmd")
public class RealmServerInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "name", type = String.class)
    public String name;

    @SQLField(column = "type", type = Integer.class)
    public int type;

    @SQLField(column = "server_host", type = String.class)
    public String serverHost;

    @SQLField(column = "server_port", type = Integer.class)
    public int serverPort;

    public RealmServerInfo() {

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
            "RealmServerInfo(name='%s', ru.infernoproject.common.server=%s:%d, type=%d)",
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
