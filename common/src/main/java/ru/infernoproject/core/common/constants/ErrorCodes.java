package ru.infernoproject.core.common.constants;

public class ErrorCodes {
    public static final byte SUCCESS = 0x00;

    public static final byte AUTH_ERROR = 0x01;
    public static final byte ALREADY_EXISTS = 0x01;
    public static final byte CHARACTER_NOT_EXISTS = 0x01;

    public static final byte AUTH_INVALID = 0x02;
    public static final byte AUTH_REQUIRED = 0x02;

    public static final byte UNKNOWN_COMMAND = 0x7D;

    public static final byte UNKNOWN_OPCODE = 0x7E;

    public static final byte SQL_ERROR = 0x7F;
}
