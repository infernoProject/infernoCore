package ru.infernoproject.core.common.net;

import java.util.Random;

public class SessionHelper {

    private static final Random random = new Random();

    public static byte[] generateSessionKey() {
        byte[] sessionKey = new byte[16];
        random.nextBytes(sessionKey);

        return sessionKey;
    }
}
