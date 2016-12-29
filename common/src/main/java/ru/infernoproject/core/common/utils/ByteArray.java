package ru.infernoproject.core.common.utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ByteArray implements ByteConvertible {

    private final List<byte[]> byteList;

    public ByteArray() {
        byteList = new ArrayList<>();
    }

    public ByteArray put(byte value) {
        byteList.add(new byte[] { value });

        return this;
    }

    public ByteArray put(ByteBuffer value) {
        byteList.add(value.array());

        return this;
    }

    public ByteArray put(byte[] value) {
        return put(
            ByteBuffer.allocate(4 + value.length)
                .putInt(value.length)
                .put(value)
        );
    }

    public ByteArray put(Integer value) {
        return put(ByteBuffer.allocate(4).putInt(value));
    }

    public ByteArray put(Long value) {
        return put(ByteBuffer.allocate(8).putLong(value));
    }

    public ByteArray put(BigInteger value) {
        return put(value.toByteArray());
    }

    public ByteArray put(ByteConvertible value) {
        return put(value.toByteArray());
    }

    public ByteArray put(List<? extends ByteConvertible> valueList) {
        put(valueList.size());

        for (ByteConvertible value: valueList) {
            put(value);
        }

        return this;
    }

    public ByteArray put(String value) {
        return put(value.getBytes());
    }

    public ByteArray put(String[] values) {
        put(values.length);

        for (String value: values) {
            put(value);
        }

        return this;
    }

    @Override
    public byte[] toByteArray() {
        final int[] size = new int[] { 0 };

        byteList.forEach(bytes -> size[0] += bytes.length);

        ByteBuffer valueBuffer = ByteBuffer.allocate(size[0]);

        for (byte[] bytes: byteList) {
            valueBuffer.put(bytes);
        }

        return valueBuffer.array();
    }

    @Override
    public String toString() {
        return HexBin.encode(toByteArray());
    }
}
