package ru.infernoproject.worldd.constants;

public class WorldEventType {
    public static final byte ENTER = 0x01;
    public static final byte LEAVE = 0x02;

    public static final byte SUBSCRIBE = 0x03;

    public static final byte MOVE = 0x04;

    public static final byte STATUS_CHANGE = 0x05;
    public static final byte HP_CHANGE = 0x05;

    public static final byte CHAT_MESSAGE = 0x06;

    private WorldEventType() {
        // Prevent class instantiation
    }
}
