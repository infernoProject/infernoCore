package ru.infernoproject.worldd.world.creature;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.worldd.world.object.WorldObjectType;

public class WorldCreature extends WorldObject {

    private int level = 1;
    private long currentHitPoints = 10;
    private long maxHitPoints = 10;
    private WorldCreatureStatus status = WorldCreatureStatus.ALIVE;

    public WorldCreature(WorldNotificationListener notificationListener, String name) {
        super(notificationListener, name);

        setType(WorldObjectType.CREATURE);
    }

    @Override
    public ByteArray getAttributes() {
        return super.getAttributes()
            .put(level)
            .put(currentHitPoints)
            .put(maxHitPoints)
            .put(status.toString().toLowerCase());
    }

    public long getCurrentHitPoints() {
        return currentHitPoints;
    }

    protected void setCurrentHitPoints(long currentHitPoints) {
        this.currentHitPoints = currentHitPoints;
    }

    public long getMaxHitPoints() {
        return maxHitPoints;
    }

    protected void setMaxHitPoints(long maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public WorldCreatureStatus getStatus() {
        return status;
    }

    protected void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void processHitPointChange(long hitPointChange) {
        currentHitPoints = Math.min(Math.max(currentHitPoints + hitPointChange, 0), maxHitPoints);
        currentCell.onEvent(this, WorldEventType.HP_CHANGE, new ByteArray());

        if (currentHitPoints == 0)
            processStatusChange(WorldCreatureStatus.DEAD);
    }

    public void processStatusChange(WorldCreatureStatus newStatus) {
        if (status != newStatus) {
            status = newStatus;

            currentCell.onEvent(this, WorldEventType.STATUS_CHANGE, new ByteArray());
        }
    }
}
