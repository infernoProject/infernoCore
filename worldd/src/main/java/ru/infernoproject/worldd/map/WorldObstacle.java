package ru.infernoproject.worldd.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.utils.MathUtils;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.util.ArrayList;
import java.util.List;

public class WorldObstacle {

    private final List<WorldPosition> obstaclePoints = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(WorldObstacle.class);

    public WorldObstacle(Location location, ByteWrapper obstacleData) {
        for (ByteWrapper obstaclePoint: obstacleData.getList()) {
            obstaclePoints.add(new WorldPosition(
                location.id, obstaclePoint.getFloat(), 0f, obstaclePoint.getFloat(), 0f
            ));
        }
    }

    public boolean isPathInsideObstacle(WorldPosition source, WorldPosition destination) {
        try {
            return MathUtils.isPathInPolygon(source, destination, obstaclePoints);
        } catch (IllegalArgumentException e) {
            logger.error("Internal server error: unable to calculate obstacle collision: {}", e.toString());

            return false;
        }
    }


    public boolean isPointInsideObstacle(WorldPosition point) {
        return MathUtils.isPointInPolygon(point, obstaclePoints);
    }
}
