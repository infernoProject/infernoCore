package ru.infernoproject.worldd.world.object;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldCell;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.intereset.InterestArea;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.oid.OID;
import ru.infernoproject.worldd.world.oid.OIDGenerator;

import java.util.List;

public class WorldObject implements Comparable<WorldObject> {

    private final OID id;
    private String name;
    private WorldPosition position;

    private final InterestArea interestArea;
    private WorldCell currentCell;

    public WorldObject(WorldNotificationListener notificationListener, String name) {
        this.id = OIDGenerator.getOID();
        this.name = name;

        this.interestArea = new InterestArea(this, notificationListener);
    }

    public void onEvent(WorldCell cell, byte type, ByteConvertible data) {
        interestArea.onEvent(cell, type, ByteWrapper.fromBytes(data));
    }

    public OID getOID() {
        return id;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void updatePosition(WorldPosition position, WorldCell targetCell, List<WorldCell> innerInterestArea, List<WorldCell> outerInterestArea) {
        this.interestArea.updateInterestArea(targetCell, innerInterestArea, outerInterestArea);

        if (targetCell != currentCell) {
            if (currentCell != null)
                currentCell.onEvent(this, WorldEventType.LEAVE, new ByteArray().put(targetCell.getX()).put(targetCell.getY()));
            targetCell.onEvent(this, WorldEventType.ENTER, new ByteArray().put(targetCell.getX()).put(targetCell.getY()));
            currentCell = targetCell;
        }

        targetCell.onEvent(this, WorldEventType.MOVE, new ByteArray().put(position));

        this.position = position;
    }

    protected void setPosition(WorldPosition position) {
        this.position = position;
    }

    public WorldPosition getPosition() {
        return position;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object target) {
        return WorldObject.class.isAssignableFrom(target.getClass()) && id.compareTo(((WorldObject) target).id) == 0;
    }

    @Override
    public int compareTo(WorldObject target) {
        return id.compareTo(target.id);
    }

    @Override
    public String toString() {
        return String.format("WorldObject(name='%s'): %s", name, interestArea);
    }
}
