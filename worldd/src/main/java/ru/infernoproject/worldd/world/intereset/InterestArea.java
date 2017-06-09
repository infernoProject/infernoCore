package ru.infernoproject.worldd.world.intereset;

import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldCell;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.worldd.world.oid.OID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InterestArea {

    private final List<OID> interestObject = new CopyOnWriteArrayList<>();

    private final List<WorldCell> innerInterestArea = new CopyOnWriteArrayList<>();
    private final List<WorldCell> outerInterestArea = new CopyOnWriteArrayList<>();

    private final WorldNotificationListener notificationListener;
    private final WorldObject object;

    private WorldCell center;

    public InterestArea(WorldObject object, WorldNotificationListener notificationListener) {
        this.object = object;
        this.notificationListener = notificationListener;
    }

    public void updateInterestArea(WorldCell center, List<WorldCell> innerInterestArea, List<WorldCell> outerInterestArea) {
        this.center = center;

        this.innerInterestArea.parallelStream()
            .filter(cell -> !innerInterestArea.contains(cell) && !outerInterestArea.contains(cell))
            .forEach(cell -> cell.unSubscribe(object));

        this.outerInterestArea.parallelStream()
            .filter(cell -> !innerInterestArea.contains(cell) && !outerInterestArea.contains(cell))
            .forEach(cell -> cell.unSubscribe(object));

        innerInterestArea.parallelStream()
            .filter(cell -> !cell.isSubscribed(object))
            .forEach(cell -> cell.subscribe(object));

        outerInterestArea.parallelStream()
            .filter(cell -> !cell.isSubscribed(object))
            .forEach(cell -> cell.subscribe(object));

        this.innerInterestArea.clear();
        this.outerInterestArea.clear();

        this.innerInterestArea.addAll(innerInterestArea);
        this.outerInterestArea.addAll(outerInterestArea);
    }

    public void onEvent(WorldCell cell, byte type, ByteWrapper data) {
        OID source = OID.fromLong(data.getLong());
        data.rewind();

        switch (type) {
            case WorldEventType.SUBSCRIBE:
                onSubscribe(cell, source, data);
                break;
            case WorldEventType.ENTER:
                onEnter(cell, source, data);
                break;
            case WorldEventType.LEAVE:
                onLeave(cell, source, data);
                break;
            default:
                if (interestObject.contains(source)) {
                    sendEvent(type, data);
                }
                break;
        }
    }

    private void onSubscribe(WorldCell cell, OID source, ByteWrapper eventData) {
        if (cell == center) {
            sendEvent(WorldEventType.SUBSCRIBE, eventData);
        }
    }

    private void onEnter(WorldCell cell, OID source, ByteWrapper eventData) {
        if (innerInterestArea.contains(cell) && !interestObject.contains(source)) {
            interestObject.add(source);

            sendEvent(WorldEventType.ENTER, eventData);
        }
    }

    private void onLeave(WorldCell cell, OID source, ByteWrapper eventData) {
        eventData.skip(8);
        ByteWrapper cellData = ByteWrapper.fromBytes(eventData.getWrapper());
        eventData.rewind();

        WorldCell newCell = new WorldCell(cellData.getInt(), cellData.getInt());

        if (!innerInterestArea.contains(newCell)&&!outerInterestArea.contains(newCell)) {
            interestObject.remove(source);
            sendEvent(WorldEventType.LEAVE, eventData);
        }
    }

    private void sendEvent(byte type, ByteConvertible data) {
        if (notificationListener != null) {
            notificationListener.onEvent(type, data);
        }
    }

    @Override
    public String toString() {
        return String.format("InterestArea(inner=%s,outer=%s)", innerInterestArea, outerInterestArea);
    }
}
