package ru.infernoproject.core.common.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

public class ByteUtils {

    public static ByteBuffer wrap(byte errCode, List<? extends ByteConvertible> byteConvertible) {
        return wrap(errCode, toByteArray(byteConvertible));
    }

    public static ByteBuffer wrap(byte errCode, ByteConvertible byteConvertible) {
        return wrap(errCode, byteConvertible.toByteArray());
    }

    public static ByteBuffer wrap(byte errCode, ByteBuffer dataBuffer) {
        return wrap(errCode, dataBuffer.array());
    }

    public static ByteBuffer wrap(byte errCode, byte... data) {
        ByteBuffer wrapper = ByteBuffer.allocate(1 + data.length);

        wrapper.put(errCode);
        wrapper.put(data);

        return wrapper;
    }

    public static byte[] getBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return bytes;
    }

    public static String getString(ByteBuffer buffer) {
        return new String(getBytes(buffer));
    }

    public static BigInteger getBigInteger(ByteBuffer buffer) {
        return new BigInteger(getBytes(buffer));
    }

    public static byte[] toByteArray(List<? extends ByteConvertible> byteConvertible) {
        final int[] byteCount = new int[] { 4 };
        List<byte[]> bytes = byteConvertible.stream()
            .map(ByteConvertible::toByteArray)
            .map(byteArray -> {
                byteCount[0] += byteArray.length + 4;
                return byteArray;
            }).collect(Collectors.toList());

        ByteBuffer byteBuffer = ByteBuffer.allocate(byteCount[0]);
        byteBuffer.putInt(bytes.size());

        for (byte[] byteArray: bytes) {
            byteBuffer.putInt(byteArray.length);
            byteBuffer.put(byteArray);
        }

        return byteBuffer.array();
    }
}
