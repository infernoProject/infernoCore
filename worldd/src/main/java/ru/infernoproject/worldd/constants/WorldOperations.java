package ru.infernoproject.worldd.constants;

public class WorldOperations {
    public static final byte AUTHORIZE = 0x00;
    public static final byte EXECUTE = 0x01;

    public static final byte MOVE = 0x30;

    public static final byte SCRIPT_LIST = 0x79;
    public static final byte SCRIPT_GET = 0x7A;
    public static final byte SCRIPT_VALIDATE = 0x7B;
    public static final byte SCRIPT_SAVE = 0x7C;

    public static final byte EVENT = 0x7D;
    public static final byte LOG_OUT = 0x7E;
    public static final byte HEART_BEAT = 0x7F;

    private WorldOperations() {
        // Prevent class instantiation
    }
}
