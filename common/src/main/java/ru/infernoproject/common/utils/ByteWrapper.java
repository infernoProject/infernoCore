package ru.infernoproject.common.utils;

import ru.infernoproject.common.utils.HexBin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ByteWrapper {

    private final ByteBuffer buffer;

    public ByteWrapper(byte[] bytes) {
        buffer = ByteBuffer.wrap(bytes);
    }

    public byte getByte() {
        return buffer.get();
    }

    public Integer getInt() {
        return buffer.getInt();
    }

    public Long getLong() {
        return buffer.getLong();
    }

    public Float getFloat() {
        return buffer.getFloat();
    }

    public Double getDouble() {
        return buffer.getDouble();
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        return bytes;
    }

    public String getString() {
        return new String(getBytes());
    }

    public BigInteger getBigInteger() {
        return new BigInteger(getBytes());
    }

    public String[] getStrings() {
        int arraySize = buffer.getInt();
        String[] array = new String[arraySize];

        for (int i = 0; i < arraySize; i++) {
            array[i] = getString();
        }

        return array;
    }

    public List<ByteWrapper> getList() {
        int listSize = buffer.getInt();
        List<ByteWrapper> list = new ArrayList<>();

        for (int i = 0; i < listSize; i++) {
            list.add(new ByteWrapper(getBytes()));
        }

        return list;
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse(getString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public ByteWrapper getWrapper() {
        return new ByteWrapper(getBytes());
    }

    public void rewind() {
        buffer.rewind();
    }

    @Override
    public String toString() {
        return HexBin.encode(buffer.array());
    }

}
