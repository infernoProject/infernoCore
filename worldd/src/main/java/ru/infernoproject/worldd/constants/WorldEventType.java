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

    public static final byte EFFECT_ADD = 0x0C;
    public static final byte EFFECT_UPDATE = 0x0D;
    public static final byte EFFECT_REMOVE = 0x0E;

    public static final byte DOT_ADD = 0x0F;
    public static final byte DOT_UPDATE = 0x10;
    public static final byte DOT_REMOVE = 0x11;

    private WorldEventType() {
        // Prevent class instantiation
    }
}
