package ru.infernoproject.worldd.constants;

public class WorldErrorCodes {

    public static final byte NOT_EXISTS = 0x01;
    public static final byte OUT_OF_RANGE = 0x02;
    public static final byte COOLDOWN = 0x03;
    public static final byte INSUFFICIENT_RESOURCES = 0x04;

    public static final byte INVALID_SCRIPT = 0x7A;
    public static final byte INVALID_REQUEST = 0x7B;
    public static final byte ILLEGAL_MOVE = 0x7C;

    private WorldErrorCodes() {
        // Prevent class instantiation
    }
}
