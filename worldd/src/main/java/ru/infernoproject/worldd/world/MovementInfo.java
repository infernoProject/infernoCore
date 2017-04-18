package ru.infernoproject.worldd.world;

import ru.infernoproject.common.utils.ByteWrapper;

import static ru.infernoproject.worldd.constants.WorldSize.MAP_HALFSIZE;

public class MovementInfo {

    private final WorldPosition position;

    public MovementInfo(WorldPosition position) {
        this.position = position;
    }

    public static MovementInfo read(ByteWrapper request) {
        WorldPosition position = new WorldPosition(
            request.getFloat(),
            request.getFloat(),
            request.getFloat(),
            request.getFloat()
        );

        return new MovementInfo(position);
    }

    public WorldPosition position() {
        return position;
    }


    public boolean validatePosition() {
        return Float.isFinite(position.getOrientation()) &&
            Float.isFinite(position.getX()) &&
            (Math.abs(position.getX()) <= MAP_HALFSIZE - 0.5f) &&
            Float.isFinite(position.getY()) &&
            (Math.abs(position.getY()) <= MAP_HALFSIZE - 0.5f) &&
            Float.isFinite(position.getZ());
    }

}
