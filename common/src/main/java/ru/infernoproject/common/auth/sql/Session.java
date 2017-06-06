package ru.infernoproject.common.auth.sql;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.utils.HexBin;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.Random;

@SQLObject(database = "realmd", table = "sessions")
public class Session implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "account")
    public Account account;

    @SQLField(column = "session_key")
    public byte[] sessionKey;

    @SQLField(column = "session_address")
    public String address = null;

    @SQLField(column = "last_activity")
    public LocalDateTime lastActivity;

    @SQLField(column = "vector")
    public byte[] vector;

    @SQLField(column = "character_id")
    public CharacterInfo characterInfo;

    public Session() {
        // Default constructor for SQLObjectWrapper
    }

    public Session(Account account, byte[] sessionKey, SocketAddress remoteAddress) {
        this.account = account;
        this.sessionKey = sessionKey;
        this.address = remoteAddress.toString();

        this.vector = generateVector();
    }

    private byte[] generateVector() {
        byte[] vector = new byte[32];

        new Random().nextBytes(vector);

        return vector;
    }

    public byte[] getKey() {
        return sessionKey;
    }

    public String getKeyHex() {
        return HexBin.encode(sessionKey);
    }

    public Account getAccount() {
        return account;
    }

    public byte[] getVector() {
        return vector;
    }

    public void setAddress(SocketAddress address) {
        this.address = address.toString();
    }
}
