package ru.infernoproject.common.auth.impl;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import java.net.SocketAddress;

@SQLObject(database = "realmd", table = "sessions")
public class Session implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "account", type = Account.class)
    public Account account;

    @SQLField(column = "session_key", type = String.class)
    public String sessionKey;

    @SQLField(column = "session_address", type = String.class)
    public String address = null;

    @SQLField(column = "last_activity", type = String.class)
    public String lastActivity = "1971-01-01 00:00:01";

    public Session() {

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
