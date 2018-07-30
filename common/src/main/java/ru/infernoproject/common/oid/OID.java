package ru.infernoproject.common.oid;

public class OID implements Comparable<OID> {

    private final long value;

    OID(long value) {
        this.value = value;
    }

    @Override
    public int compareTo(OID target) {
        return Long.compare(this.value, target.value);
    }

    public Long toLong() {
        return value;
    }

    public static OID fromLong(long value) {
        return new OID(value);
    }

    @Override
    public boolean equals(Object target) {
        return OID.class.isAssignableFrom(target.getClass()) && value == ((OID) target).value;
    }

    @Override
    public int hashCode() {
        return (int) (value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("OID(%d)", value);
    }
}
