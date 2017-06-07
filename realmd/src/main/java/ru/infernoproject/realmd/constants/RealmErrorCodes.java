package ru.infernoproject.realmd.constants;

public class RealmErrorCodes {

    public static final byte ALREADY_EXISTS = 0x01;
    public static final byte CHARACTER_EXISTS = 0x01;

    public static final byte AUTH_INVALID = 0x02;
    public static final byte CHARACTER_NOT_FOUND = 0x02;

    public static final byte USER_BANNED = 0x03;
    public static final byte CHARACTER_DELETED = 0x03;

    private RealmErrorCodes() {
        // Prevent class instantiation
    }
}
