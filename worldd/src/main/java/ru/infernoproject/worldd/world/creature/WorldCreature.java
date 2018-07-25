package ru.infernoproject.worldd.world.creature;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.worldd.world.object.WorldObjectType;

public class WorldCreature extends WorldObject {

    private long currentHitPoints = 0;
    private long maxHitPoints = 0;

    public WorldCreature(WorldNotificationListener notificationListener, String name) {
        super(notificationListener, name);

        setType(WorldObjectType.CREATURE);
    }

    @Override
    public ByteArray getAttributes() {
        return super.getAttributes()
            .put(currentHitPoints)
            .put(maxHitPoints);
    }

    public long getCurrentHitPoints() {
        return currentHitPoints;
    }

    public void setCurrentHitPoints(long currentHitPoints) {
        this.currentHitPoints = currentHitPoints;
    }

    public long getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(long maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }
}
