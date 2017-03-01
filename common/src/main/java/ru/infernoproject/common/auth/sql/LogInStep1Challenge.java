package ru.infernoproject.common.auth.sql;

import java.math.BigInteger;

public class LogInStep1Challenge {

    private final boolean success;

    private BigInteger salt;
    private BigInteger B;
    private Session session;

    public LogInStep1Challenge() {
        success = false;
    }

    public LogInStep1Challenge(Session session, BigInteger salt, BigInteger B) {
        success = true;

        this.session = session;
        this.salt = salt;
        this.B = B;
    }

    public boolean isSuccess() {
        return success;
    }

    public BigInteger getSalt() {
        return salt;
    }

    public BigInteger getB() {
        return B;
    }

    public Session getSession() {
        return session;
    }
}
