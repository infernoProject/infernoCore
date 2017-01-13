package ru.infernoproject.core.common.types.auth;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

public class Session {

    private final int id;
    private final Account account;
    private final byte[] sessionKey;

    public Session(int id, Account account, byte[] sessionKey) {
        this.id = id;
        this.account = account;
        this.sessionKey = sessionKey;
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
}
