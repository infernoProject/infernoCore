package ru.infernoproject.worldd.constants;

public class WorldOperations {
    public static final byte AUTHORIZE = 0x00;
    public static final byte EXECUTE = 0x01;
    public static final byte CHAT_MESSAGE = 0x02;

    public static final byte MOVE_START_FORWARD = 0x30;
    public static final byte MOVE_START_BACKWARD = 0x31;
    public static final byte MOVE_STOP = 0x32;
    public static final byte MOVE_START_STRAFE_LEFT = 0x33;
    public static final byte MOVE_START_STRAFE_RIGHT = 0x34;
    public static final byte MOVE_STOP_STRAFE = 0x35;
    public static final byte MOVE_JUMP = 0x36;
    public static final byte MOVE_START_TURN_LEFT = 0x37;
    public static final byte MOVE_START_TURN_RIGHT = 0x38;
    public static final byte MOVE_STOP_TURN = 0x39;
    public static final byte MOVE_START_PITCH_UP = 0x3A;
    public static final byte MOVE_START_PITCH_DOWN = 0x3B;
    public static final byte MOVE_STOP_PITCH = 0x3C;
    public static final byte MOVE_SET_RUN_MODE = 0x3D;
    public static final byte MOVE_SET_WALK_MODE = 0x3E;
    public static final byte MOVE_FALL_LAND = 0x3F;
    public static final byte MOVE_START_SWIM = 0x40;
    public static final byte MOVE_STOP_SWIM = 0x41;
    public static final byte MOVE_SET_FACING = 0x42;
    public static final byte MOVE_SET_PITCH = 0x43;

    public static final byte EVENT = 0x7D;
    public static final byte LOG_OUT = 0x7E;
    public static final byte HEART_BEAT = 0x7F;

    private WorldOperations() {
        // Prevent class instantiation
    }
}
