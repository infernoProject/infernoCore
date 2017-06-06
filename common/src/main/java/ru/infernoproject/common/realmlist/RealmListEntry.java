package ru.infernoproject.common.realmlist;

import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteConvertible;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;

@SQLObject(table = "realm_list", database = "realmd")
public class RealmListEntry implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "type")
    public int type;

    @SQLField(column = "server_host")
    public String serverHost;

    @SQLField(column = "server_port")
    public int serverPort;

    @SQLField(column = "online")
    public int online;

    @SQLField(column = "last_seen")
    public LocalDateTime lastSeen;

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
            "RealmListEntry(name='%s', host=%s:%d, type=%d)",
            name, serverHost, serverPort, type
        );
    }

    public void update() {
        lastSeen = LocalDateTime.now();
        online = 1;
    }
}
