package ru.infernoproject.worldd.world.movement;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.constants.WorldSize;

public class WorldPosition implements ByteConvertible {

    private final int location;

    private final float x;
    private final float y;
    private final float z;

    private final float orientation;

    public WorldPosition(int location, float x, float y, float z, float orientation) {
        validate(x, y, z, orientation);

        this.location = location;
        this.x = x;
        this.y = y;
        this.z = z;
        this.orientation = orientation;
    }

    private void validate(float x, float y, float z, float orientation) {
        boolean xIsValid = Float.isFinite(x) && Math.abs(x) < WorldSize.MAP_HALFSIZE;
        boolean yIsValid = Float.isFinite(y) && Math.abs(y) < WorldSize.MAP_HALFSIZE;
        boolean zIsValid = Float.isFinite(z);

        boolean orientationIsValid = Float.isFinite(orientation) && 0f <= orientation && orientation < 360f;

        if (!xIsValid || !yIsValid || !zIsValid || !orientationIsValid)
            throw new IllegalArgumentException("Invalid position");
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
