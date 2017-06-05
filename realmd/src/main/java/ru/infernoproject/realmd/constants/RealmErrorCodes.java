package ru.infernoproject.realmd.constants;

public class RealmErrorCodes {

    public static final byte SUCCESS = 0x00;

    public static final byte AUTH_ERROR = 0x01;
    public static final byte ALREADY_EXISTS = 0x01;
    public static final byte CHARACTER_EXISTS = 0x01;

    public static final byte AUTH_INVALID = 0x02;
    public static final byte AUTH_REQUIRED = 0x02;
    public static final byte CHARACTER_NOT_FOUND = 0x02;

    public static final byte CHARACTER_DELETED = 0x03;

    public static final byte SERVER_ERROR = 0x7F;

    private RealmErrorCodes() {
        // Prevent class instantiation
    }
}
