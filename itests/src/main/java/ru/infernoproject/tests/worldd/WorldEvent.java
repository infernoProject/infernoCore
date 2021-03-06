package ru.infernoproject.tests.worldd;

import ru.infernoproject.common.oid.OID;
import ru.infernoproject.common.utils.ByteWrapper;

public class WorldEvent {

    private final byte eventType;

    private final OID objectId;
    private final String objectType;
    private final String objectName;

    private final ByteWrapper objectData;
    private final ByteWrapper eventData;

    public WorldEvent(ByteWrapper eventWrapper) {
        eventType = eventWrapper.getByte();

        ByteWrapper eventDataWrapper = eventWrapper.getWrapper();

        objectData = eventDataWrapper.getWrapper();

        objectId = objectData.getOID();
        objectType = objectData.getString();
        objectName = objectData.getString();

        eventData = eventDataWrapper.getWrapper();
    }

    public byte getEventType() {
        return eventType;
    }

    public OID getObjectId() {
        return objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public ByteWrapper getEventData() {
        return eventData;
    }

    public String getObjectType() {
        return objectType;
    }

    public ByteWrapper getObjectData() {
        return objectData;
    }
}
