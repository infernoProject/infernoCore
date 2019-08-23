package ru.infernoproject.worldd.world.interest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldCell;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.common.oid.OID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InterestArea {

    private final List<OID> interestObject = new CopyOnWriteArrayList<>();

    private final List<WorldCell> innerInterestArea = new CopyOnWriteArrayList<>();
    private final List<WorldCell> outerInterestArea = new CopyOnWriteArrayList<>();

    private final WorldNotificationListener notificationListener;
    private final WorldObject object;

    private WorldCell center;

    private static final Logger logger = LoggerFactory.getLogger(InterestArea.class);

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
        ByteWrapper sourceData = data.getWrapper();
        OID source = sourceData.getOID();
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
            case WorldEventType.CHAT_MESSAGE:
                if (cell == center) {
                    sendEvent(type, data);
                }
                break;
            case WorldEventType.INVITE:
            case WorldEventType.INVITE_RESPONSE:
            case WorldEventType.DOT_ADD:
            case WorldEventType.DOT_REMOVE:
            case WorldEventType.EFFECT_ADD:
            case WorldEventType.EFFECT_REMOVE:
                sendEvent(type, data);
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
            logger.debug("{} subscribed for {} updates", object, source);
        }
    }

    private void onLeave(WorldCell cell, OID source, ByteWrapper eventData) {
        ByteWrapper sourceData = eventData.getWrapper();
        ByteWrapper cellData = eventData.getWrapper();
        eventData.rewind();

        WorldCell newCell = new WorldCell(cellData.getInt(), cellData.getInt());

        if (!(innerInterestArea.contains(newCell)||outerInterestArea.contains(newCell))&&interestObject.contains(source)) {
            interestObject.remove(source);
            sendEvent(WorldEventType.LEAVE, eventData);

            logger.debug("{} unsubscribed from {} updates", object, source);
        }
    }

    private void sendEvent(byte type, ByteConvertible data) {
        if (notificationListener != null) {
            notificationListener.onEvent(type, data);
        }
    }

    public void destroy() {
        this.innerInterestArea.parallelStream()
            .forEach(cell -> cell.unSubscribe(object));

        this.outerInterestArea.parallelStream()
            .forEach(cell -> cell.unSubscribe(object));
    }

    @Override
    public String toString() {
        return String.format("InterestArea(inner=%s,outer=%s)", innerInterestArea, outerInterestArea);
    }
}
