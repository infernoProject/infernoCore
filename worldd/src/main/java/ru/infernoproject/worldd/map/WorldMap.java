package ru.infernoproject.worldd.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.util.ArrayList;
import java.util.List;

public class WorldMap {

    private final Location location;
    private final WorldCell[][] cells = new WorldCell[WorldSize.CELL_TOTAL][WorldSize.CELL_TOTAL];

    private static final Logger logger = LoggerFactory.getLogger(WorldMap.class);

    public WorldMap(Location location, ByteWrapper mapData) {
        this.location = location;

        for (int x = 0; x < WorldSize.CELL_TOTAL; x++) {
            cells[x] = new WorldCell[WorldSize.CELL_TOTAL];

            for (int y = 0; y < WorldSize.CELL_TOTAL; y++) {
                cells[x][y] = new WorldCell(x, y);
            }
        }
    }

    public WorldCell getCellByPosition(WorldPosition position) {
        return getCellByPosition(position.getX(), position.getY());
    }

    public WorldCell getCellByPosition(float positionX, float positionY) {
        int x = Math.min((int) Math.floor(positionX / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID, WorldSize.CELL_TOTAL - 1);
        int y = Math.min((int) Math.floor(positionY / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID, WorldSize.CELL_TOTAL - 1);

        return cells[x][y];
    }

    public List<WorldCell> calculateInnerInterestArea(WorldPosition position) {
        WorldCell bottomLeft = getCellByPosition(
            Math.max(position.getX() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE),
            Math.max(position.getY() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE)
        );
        WorldCell topRight = getCellByPosition(
            Math.min(position.getX() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE),
            Math.min(position.getY() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE)
        );

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (int y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                interestArea.add(cells[x][y]);
            }
        }

        return interestArea;
    }

    public List<WorldCell> calculateOuterInterestArea(WorldPosition position) {
        WorldCell bottomLeft = getCellByPosition(
            Math.max(position.getX() - WorldSize.OUTER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE),
            Math.max(position.getY() - WorldSize.OUTER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE)
        );
        WorldCell topRight = getCellByPosition(
            Math.min(position.getX() + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE),
            Math.min(position.getY() + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE)
        );

        WorldCell bottomLeftInner = getCellByPosition(
            Math.max(position.getX() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE),
            Math.max(position.getY() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE)
        );
        WorldCell topRightInner = getCellByPosition(
            Math.min(position.getX() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE),
            Math.min(position.getY() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE)
        );

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (int y = bottomLeft.getY(); y <= topRight.getY(); y++) {
                if (x < bottomLeftInner.getX() || x > topRightInner.getX() || y < bottomLeftInner.getY() || y > topRightInner.getY()) {
                    interestArea.add(cells[x][y]);
                }
            }
        }

        return interestArea;
    }

    public Location getLocation() {
        return location;
    }

    public static float calculateDistance(WorldPosition a, WorldPosition b) {
        float distanceX = a.getX() - b.getX();
        float distanceY = a.getY() - b.getY();
        float distanceZ = a.getZ() - b.getZ();

        return (float) Math.sqrt(
            Math.pow(distanceX, 2.0f) + Math.pow(distanceY, 2.0) + Math.pow(distanceZ, 2.0)
        );
    }

    public boolean isLegalMove(WorldPosition currentPosition, WorldPosition newPosition) {
        float distance = calculateDistance(currentPosition, newPosition);

        if (distance > WorldSize.MAX_SPEED)
            return false;

        // TODO: Implement move validation

        return true;
    }
}
