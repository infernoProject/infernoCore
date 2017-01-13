package ru.infernoproject.core.common.types.auth;

import java.math.BigInteger;

public class LogInStep2Challenge {

    private final BigInteger M2;
    private final boolean success;

    public LogInStep2Challenge() {
        this.M2 = BigInteger.ZERO;
        this.success = false;
    }

    public LogInStep2Challenge(BigInteger M2) {
        this.M2 = M2;
        this.success = true;
    }

    public BigInteger getM2() {
        return M2;
    }

    public boolean isSuccess() {
        return success;
    }
}
