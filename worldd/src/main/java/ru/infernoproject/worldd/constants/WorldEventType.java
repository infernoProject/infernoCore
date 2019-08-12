package ru.infernoproject.worldd.constants;

public class WorldEventType {
    public static final byte ENTER = 0x01;
    public static final byte LEAVE = 0x02;

    public static final byte SUBSCRIBE = 0x03;

    public static final byte MOVE = 0x04;

    public static final byte STATUS_CHANGE = 0x05;
    public static final byte HP_CHANGE = 0x06;

    public static final byte CHAT_MESSAGE = 0x07;

    public static final byte INVITE = 0x08;
    public static final byte INVITE_RESPONSE = 0x09;

    public static final byte TIME_CHANGE = 0x0A;
    public static final byte WEATHER_CHANGE = 0x0B;

    private WorldEventType() {
        // Prevent class instantiation
    }
}
