package ru.linachan.inferno.world.interest;

import ru.linachan.inferno.common.vector.Vector2;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;

public class InterestArea {

    private Vector2<Integer, Integer> topLeft;
    private Vector2<Integer, Integer> bottomRight;

    private List<InterestObject> interestTargets = new CopyOnWriteArrayList<>();
    private BlockingQueue<byte[]> eventQueue = new SynchronousQueue<>();

    public void setArea(Vector2<Integer, Integer> topLeftCorner, Vector2<Integer, Integer> bottomRightCorner) {
        topLeft = topLeftCorner;
        bottomRight = bottomRightCorner;
    }

    public void onEvent(InterestEvent event) {
        InterestObject interestTarget = event.getTarget();
        byte[] eventData = event.getData().array();

        ByteBuffer eventBuffer = null;
        switch (event.getType()) {
            case ENTER:
                interestTargets.add(interestTarget);
                eventBuffer = ByteBuffer.allocate(interestTarget.getName().length() + eventData.length + 42);

                eventBuffer.put((byte) 1);

                eventBuffer.putLong(interestTarget.getID().getLeastSignificantBits());
                eventBuffer.putLong(interestTarget.getID().getMostSignificantBits());

                eventBuffer.putDouble(interestTarget.getPosition().getX());
                eventBuffer.putDouble(interestTarget.getPosition().getY());

                eventBuffer.putInt(interestTarget.getName().length());
                eventBuffer.put(interestTarget.getName().getBytes());

                eventBuffer.putInt(eventData.length);
                eventBuffer.put(eventData);
                break;
            case LEAVE:
                interestTargets.remove(interestTarget);
                eventBuffer = ByteBuffer.allocate(17);

                eventBuffer.put((byte) 3);

                eventBuffer.putLong(interestTarget.getID().getLeastSignificantBits());
                eventBuffer.putLong(interestTarget.getID().getMostSignificantBits());
                break;
            case UPDATE:
                eventBuffer = ByteBuffer.allocate(eventData.length + 37);

                eventBuffer.put((byte) 2);

                eventBuffer.putLong(interestTarget.getID().getLeastSignificantBits());
                eventBuffer.putLong(interestTarget.getID().getMostSignificantBits());

                eventBuffer.putDouble(interestTarget.getPosition().getX());
                eventBuffer.putDouble(interestTarget.getPosition().getY());

                eventBuffer.putInt(eventData.length);
                eventBuffer.put(eventData);
                break;
            case MESSAGE:
                eventBuffer = ByteBuffer.allocate(eventData.length + 21);

                eventBuffer.put((byte) 4);

                if (interestTarget != null) {
                    eventBuffer.putLong(interestTarget.getID().getLeastSignificantBits());
                    eventBuffer.putLong(interestTarget.getID().getMostSignificantBits());
                } else {
                    eventBuffer.putLong(0);
                    eventBuffer.putLong(0);
                }

                eventBuffer.putInt(eventData.length);
                eventBuffer.put(eventData);
                break;
        }

        try { eventQueue.put(eventBuffer.array()); } catch(InterruptedException ignored) {}
    }

    public boolean isSubscribed(InterestObject interestTarget) {
        return interestTargets.contains(interestTarget);
    }

    public boolean isInterestedIn(Vector2<Integer, Integer> targetRegion) {
        boolean xInside = (topLeft.getX() <= targetRegion.getX()) && (targetRegion.getX() <= bottomRight.getX());
        boolean yInside = (topLeft.getY() <= targetRegion.getY()) && (targetRegion.getY() <= bottomRight.getY());

        return xInside && yInside;
    }

    public List<byte[]> getEvents() {
        List<byte[]> events = new CopyOnWriteArrayList<>();
        while (!eventQueue.isEmpty()) {
            try { events.add(eventQueue.take()); } catch(InterruptedException ignored) {}
        }
        return events;
    }
}
