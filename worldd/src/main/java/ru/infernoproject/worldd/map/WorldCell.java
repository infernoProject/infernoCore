package ru.infernoproject.worldd.map;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.common.oid.OID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldCell {

    private final int x;
    private final int z;

    private final List<WorldObject> subscribers = new CopyOnWriteArrayList<>();

    public WorldCell(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public void subscribe(WorldObject subscriber) {
        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber);

            onEvent(subscriber, WorldEventType.SUBSCRIBE, null);
        }
    }

    public void unSubscribe(WorldObject subscriber) {
        subscribers.remove(subscriber);
    }

    public boolean isSubscribed(WorldObject object) {
        return subscribers.contains(object);
    }

    public synchronized void onEvent(WorldObject source, byte eventType, ByteConvertible eventData) {
        subscribers.parallelStream()
            .filter(subscriber -> !subscriber.equals(source))
            .forEach(subscriber -> subscriber.onEvent(
                this, eventType,
                new ByteArray()
                    .put(source.getAttributes())
                    .put(eventData)
            ));
    }

    public WorldObject findObjectById(OID id) {
        return subscribers.parallelStream()
            .filter(worldObject -> worldObject.getOID().equals(id))
            .findFirst().orElse(null);
    }

    @Override
    public boolean equals(Object target) {
        if (WorldCell.class.isAssignableFrom(target.getClass())) {
            WorldCell targetCell = (WorldCell) target;

            return (targetCell.x == x) && (targetCell.z == z);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = x & 0xFFFF;

        hash <<= 16;
        hash &= z & 0xFFFF;

        return hash;
    }

    @Override
    public String toString() {
        return String.format("WorldCell[%d:%d]", x, z);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public List<WorldObject> getSubscribers() {
        return subscribers;
    }
}
