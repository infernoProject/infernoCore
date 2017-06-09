package ru.infernoproject.worldd.world.movement;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

public class WorldPosition implements ByteConvertible {

    private final int location;

    private final float x;
    private final float y;
    private final float z;

    private final float orientation;

    public WorldPosition(int location, float x, float y, float z, float orientation) {
        this.location = location;
        this.x = x;
        this.y = y;
        this.z = z;
        this.orientation = orientation;
    }

    public int getLocation() {
        return location;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getOrientation() {
        return orientation;
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(x).put(y).put(z)
            .put(orientation)
            .toByteArray();
    }
}
