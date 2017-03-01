package ru.infernoproject.worldd.world;

public class WorldPosition {

    private final float x;
    private final float y;
    private final float z;

    private final float orientation;

    public WorldPosition(float x, float y, float z, float orientation) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.orientation = orientation;
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
}
