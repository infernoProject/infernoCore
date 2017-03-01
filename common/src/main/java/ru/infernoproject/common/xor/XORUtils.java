package ru.infernoproject.common.xor;

import java.nio.ByteBuffer;
import java.util.Random;

public class XORUtils {

    private static final byte[] BIN_KEY = new byte[] { 0x55 };
    private static final Random RAND = new Random();
    private static final int KEY_LENGTH = 8;

    private static byte[] xor(byte[] data, byte[] key) {
        byte[] xorData = new byte[data.length];

        for (int i = 0; i < xorData.length; i++) {
            xorData[i] = (byte) ((data[i] ^ key[i % key.length]) & 0xFF);
        }

        return xorData;
    }

    public static byte[] xencode(byte[] data) {
        ByteBuffer xencoded = ByteBuffer.allocate(data.length + KEY_LENGTH);

        byte[] key = new byte[KEY_LENGTH];
        RAND.nextBytes(key);

        xencoded.put(xor(key, BIN_KEY));
        xencoded.put(xor(data, key));

        return xencoded.array();
    }

    public static byte[] xdecode(byte[] data) {
        ByteBuffer xencoded = ByteBuffer.wrap(data);

        byte[] key = new byte[KEY_LENGTH];
        xencoded.get(key);
        key = xor(key, BIN_KEY);

        byte[] xdecoded = new byte[xencoded.remaining()];
        xencoded.get(xdecoded);
        return xor(xdecoded, key);
    }
}
