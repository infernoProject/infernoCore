package ru.linachan.inferno.world.interest;

import java.nio.ByteBuffer;

public class InterestEvent {

    private InterestEventType type;
    private InterestObject target;
    private ByteBuffer data = ByteBuffer.allocate(0);

    public InterestEvent(InterestEventType eventType, InterestObject eventTarget) {
        type = eventType;
        target = eventTarget;
    }

    public void setEventData(ByteBuffer eventData) {
        data = eventData;
    }

    public InterestEventType getType() {
        return type;
    }

    public InterestObject getTarget() {
        return target;
    }

    public ByteBuffer getData() {
        return data;
    }
}
