package ru.infernoproject.realmd.constants;

public class RealmOperations {

    public static final byte CRYPTO_CONFIG = 0x00;

    public static final byte SIGN_UP = 0x01;
    public static final byte LOG_IN_STEP1 = 0x02;
    public static final byte LOG_IN_STEP2 = 0x03;

    public static final byte REALM_LIST = 0x04;

    public static final byte SESSION_TOKEN = 0x05;

    public static final byte CHARACTER_LIST = 0x06;
    public static final byte CHARACTER_CREATE = 0x07;
    public static final byte CHARACTER_DELETE = 0x08;
    public static final byte CHARACTER_SELECT = 0x09;

    public static final byte CHARACTER_RESTORABLE_LIST = 0x0A;
    public static final byte CHARACTER_RESTORE = 0x0B;
    public static final byte CHARACTER_RENAME = 0x0C;

    public static final byte RACE_LIST = 0x0D;
    public static final byte CLASS_LIST = 0x0E;

    private RealmOperations() {
        // Prevent class instantiation
    }
}
