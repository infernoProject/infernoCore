package ru.infernoproject.tests.worldd;

import ru.infernoproject.common.utils.ByteWrapper;

public class WorldEvent {

    private final byte eventType;

    private final long objectId;
    private final String objectName;

    private final ByteWrapper eventData;

    public WorldEvent(ByteWrapper eventWrapper) {
        eventType = eventWrapper.getByte();

        ByteWrapper eventDataWrapper = eventWrapper.getWrapper();
        objectId = eventDataWrapper.getLong();
        objectName = eventDataWrapper.getString();

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
}
