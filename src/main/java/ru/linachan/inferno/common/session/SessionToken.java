package ru.linachan.inferno.common.session;

import java.util.Arrays;

public class SessionToken {

    private byte[] token;

    public SessionToken(byte[] sessionToken) {
        token = sessionToken;
    }

    public byte[] getBytes() {
        return token;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(token);
    }

    @Override
    public boolean equals(Object target){
        if (target == null) return false;
        if (target == this) return true;
        if (!(target instanceof SessionToken)) return false;

        byte[] targetToken = ((SessionToken) target).getBytes();
        return Arrays.equals(token, targetToken);
    }
}
