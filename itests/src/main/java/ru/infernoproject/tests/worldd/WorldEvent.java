package ru.infernoproject.tests.worldd;

import ru.infernoproject.common.utils.ByteWrapper;

public class WorldEvent {

    private final byte eventType;

    private final long objectId;
    private final String objectType;
    private final String objectName;

    private final ByteWrapper objectData;
    private final ByteWrapper eventData;

    public WorldEvent(ByteWrapper eventWrapper) {
        eventType = eventWrapper.getByte();

        ByteWrapper eventDataWrapper = eventWrapper.getWrapper();

        objectData = eventDataWrapper.getWrapper();

        objectId = objectData.getWrapper().getLong();
        objectType = objectData.getString();
        objectName = objectData.getString();

        eventData = eventDataWrapper.getWrapper();
    }

    public byte getEventType() {
        return eventType;
    }

    public long getObjectId() {
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
