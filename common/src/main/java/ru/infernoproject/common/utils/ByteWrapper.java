package ru.infernoproject.common.utils;

import com.google.common.io.Files;
import ru.infernoproject.common.oid.OID;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ByteWrapper implements ByteConvertible {

    private final ByteBuffer buffer;

    public ByteWrapper(byte[] bytes) {
        buffer = ByteBuffer.wrap(bytes);
    }

    public byte getByte() {
        return buffer.get();
    }

    public boolean getBoolean() {
        return getByte() == 1;
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

    public OID getOID() {
        return OID.fromLong(getWrapper().getLong());
    }

    public <T extends Enum<T>> T getEnum(Class<T> enumType) {
        String value = getString().toUpperCase();
        for (T enumValue: enumType.getEnumConstants()) {
            if (enumValue.toString().equals(value)) {
                return enumValue;
            }
        }

        throw new IllegalArgumentException(String.format(
            "Enum type '%s' has no value '%s'", enumType.getSimpleName(), value
        ));
    }

    public void skip(int bytes) {
        buffer.position(buffer.position() + bytes);
    }

    public void rewind() {
        buffer.rewind();
    }

    public static ByteWrapper readFile(File file) throws IOException {
        return new ByteWrapper(Files.toByteArray(file));
    }

    public static ByteWrapper fromBytes(ByteConvertible data) {
        return new ByteWrapper(data.toByteArray());
    }

    @Override
    public String toString() {
        return HexBin.encode(buffer.array());
    }

    @Override
    public byte[] toByteArray() {
        buffer.rewind();

        return buffer.array();
    }
}
