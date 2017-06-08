package ru.infernoproject.worldd.world.oid;

public class OID implements Comparable<OID> {

    private final long value;

    OID(long value) {
        this.value = value;
    }

    @Override
    public int compareTo(OID target) {
        return Long.compare(this.value, target.value);
    }
}
