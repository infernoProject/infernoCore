package ru.infernoproject.worldd.map;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.world.object.WorldObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldCell {

    private final int x;
    private final int y;

    private final List<WorldObject> subscribers = new CopyOnWriteArrayList<>();

    public WorldCell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void subscribe(WorldObject subscriber) {
        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber);

            onEvent(subscriber, WorldEventType.SUBSCRIBE, null);
        }
    }

    public void unSubscribe(WorldObject subscriber) {
        if (subscribers.contains(subscriber)) {
            subscribers.remove(subscriber);
        }
    }

    public boolean isSubscribed(WorldObject object) {
        return subscribers.contains(object);
    }

    public void onEvent(WorldObject source, byte eventType, ByteConvertible eventData) {
        subscribers.parallelStream()
            .filter(subscriber -> !subscriber.equals(source))
            .forEach(subscriber -> subscriber.onEvent(
                this, eventType, new ByteArray().put(source.getOID().toLong()).put(eventData)
            ));
    }

    @Override
    public boolean equals(Object target) {
        if (WorldCell.class.isAssignableFrom(target.getClass())) {
            WorldCell targetCell = (WorldCell) target;

            return (targetCell.x == x) && (targetCell.y == y);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = x & 0xFFFF;

        hash <<= 16;
        hash &= y & 0xFFFF;

        return hash;
    }

    @Override
    public String toString() {
        return String.format("WorldCell[%d:%d]", x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
