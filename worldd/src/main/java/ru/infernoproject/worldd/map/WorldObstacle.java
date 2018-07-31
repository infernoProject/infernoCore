package ru.infernoproject.worldd.map;

import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.utils.MathUtils;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.util.ArrayList;
import java.util.List;

public class WorldObstacle {

    private final List<WorldPosition> obstaclePoints = new ArrayList<>();

    public WorldObstacle(Location location, ByteWrapper obstacleData) {
        for (ByteWrapper obstaclePoint: obstacleData.getList()) {
            obstaclePoints.add(new WorldPosition(
                location.id, obstaclePoint.getFloat(), obstaclePoint.getFloat(), 0f,0f
            ));
        }
    }

    public boolean isPathInsideObstacle(WorldPosition source, WorldPosition destination) {
        return MathUtils.isPathInPolygon(source, destination, obstaclePoints);
    }


    public boolean isPointInsideObstacle(WorldPosition point) {
        return MathUtils.isPointInPolygon(point, obstaclePoints);
    }
}
