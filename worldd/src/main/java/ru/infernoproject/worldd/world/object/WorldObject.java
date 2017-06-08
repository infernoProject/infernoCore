package ru.infernoproject.worldd.world.object;

import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.oid.OID;
import ru.infernoproject.worldd.world.oid.OIDGenerator;

public class WorldObject {

    private final OID id;
    private String name;
    private WorldPosition position;

    private final WorldNotificationListener notificationListener;

    public WorldObject(WorldNotificationListener notificationListener, String name) {
        this.id = OIDGenerator.getOID();

        this.notificationListener = notificationListener;
        this.name = name;
    }

    public void onEvent(byte type, ByteConvertible data) {
        if (notificationListener != null) {
            notificationListener.onEvent(type, data);
        }
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

    protected void setPosition(WorldPosition position) {
        this.position = position;
    }

    public WorldPosition getPosition() {
        return position;
    }
}
