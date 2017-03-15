package ru.infernoproject.realmd;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

import java.nio.ByteBuffer;

@SQLObject(table = "realm_list", database = "realmd")
public class RealmServerInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "type")
    public int type;

    @SQLField(column = "server_host")
    public String serverHost;

    @SQLField(column = "server_port")
    public int serverPort;

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
}
