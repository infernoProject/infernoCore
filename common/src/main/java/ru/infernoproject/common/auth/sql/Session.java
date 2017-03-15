package ru.infernoproject.common.auth.sql;

import ru.infernoproject.common.utils.HexBin;
import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import java.net.SocketAddress;

@SQLObject(database = "realmd", table = "sessions")
public class Session implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "account")
    public Account account;

    @SQLField(column = "session_key")
    public String sessionKey;

    @SQLField(column = "session_address")
    public String address = null;

    @SQLField(column = "last_activity")
    public String lastActivity = "1971-01-01 00:00:01";

    public Session() {
        // Default constructor for SQLObjectWrapper
    }

    public Session(Account account, byte[] sessionKey, SocketAddress remoteAddress) {
        this.account = account;
        this.sessionKey = HexBin.encode(sessionKey);
        this.address = remoteAddress.toString();
    }

    public byte[] getKey() {
        return HexBin.decode(sessionKey);
    }

    public String getKeyHex() {
        return sessionKey;
    }

    public Account getAccount() {
        return account;
    }

    public void setAddress(SocketAddress address) {
        this.address = address.toString();
    }
}
