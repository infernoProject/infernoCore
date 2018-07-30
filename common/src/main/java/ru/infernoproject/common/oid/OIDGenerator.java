package ru.infernoproject.common.oid;

public class OIDGenerator {

    private static final long firstOID = 1L;
    private static final long lastOID = Long.MAX_VALUE;

    private static long nextOID = firstOID;

    private OIDGenerator() {
        // Prevent class instantiation
    }

    public static OID getOID() {
        return new OID(nextOID++);
    }
}
