package ru.infernoproject.core.common.constants;

public class ErrorCodes {
    public static final byte SUCCESS = 0x00;

    public static final byte AUTH_ERROR = 0x01;
    public static final byte ALREADY_EXISTS = 0x01;
    public static final byte CHARACTER_NOT_EXISTS = 0x01;

    public static final byte AUTH_INVALID = 0x02;
    public static final byte AUTH_REQUIRED = 0x02;

    public static final byte NOT_IN_GAME = 0x03;
    public static final byte PLAYER_DEAD = 0x04;

    public static final byte SPELL_COOL_DOWN = 0x05;
    public static final byte SPELL_NOT_LEARNED = 0x06;

    public static final byte INVALID_REQUEST = 0x7B;

    public static final byte UNKNOWN_COMMAND = 0x7C;
    public static final byte UNKNOWN_OPCODE = 0x7D;

    public static final byte SCRIPT_ERROR = 0x7E;
    public static final byte SQL_ERROR = 0x7F;
}
