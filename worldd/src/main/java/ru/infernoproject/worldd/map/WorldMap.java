package ru.infernoproject.worldd.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.utils.MathUtils;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.common.oid.OID;

import java.util.*;
import java.util.stream.Collectors;

public class WorldMap {

    private final Location location;
    private final WorldCell[][] cells = new WorldCell[WorldSize.CELL_TOTAL][WorldSize.CELL_TOTAL];

    private final List<WorldObstacle> obstacles = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(WorldMap.class);

    public WorldMap(Location location, ByteWrapper mapData) {
        this.location = location;

        for (int x = 0; x < WorldSize.CELL_TOTAL; x++) {
            cells[x] = new WorldCell[WorldSize.CELL_TOTAL];

            for (int z = 0; z < WorldSize.CELL_TOTAL; z++) {
                cells[x][z] = new WorldCell(x, z);
            }
        }

        readMapData(mapData);
    }

    private void readMapData(ByteWrapper mapData) {
        for (ByteWrapper obstacleData: mapData.getList()) {
            obstacles.add(new WorldObstacle(location, obstacleData));
        }
    }

    public WorldCell getCellByPosition(WorldPosition position) {
        return getCellByPosition(position.getX(), position.getY());
    }

    public WorldCell getCellByPosition(float positionX, float positionZ) {
        int x = Math.min((int) Math.floor(positionX / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID, WorldSize.CELL_TOTAL - 1);
        int z = Math.min((int) Math.floor(positionZ / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID, WorldSize.CELL_TOTAL - 1);

        return cells[x][z];
    }

    public WorldObject findObjectById(OID id) {
        return Arrays.asList(cells).parallelStream()
            .map(worldCells -> Arrays.asList(worldCells).parallelStream()
                .map(worldCell -> worldCell.findObjectById(id))
                .collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .distinct()
            .findFirst().orElse(null);
    }

    public List<WorldObject> findObjectsInArea(WorldPosition position, float radius) {
        return Arrays.asList(cells).parallelStream()
            .map(worldCells -> Arrays.asList(worldCells).parallelStream()
                .map(WorldCell::getSubscribers)
                .flatMap(List::stream)
                .collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .distinct()
            .filter(worldObject -> MathUtils.calculateDistance(position, worldObject.getPosition()) <= radius)
            .collect(Collectors.toList());
    }

    public List<WorldCell> calculateInnerInterestArea(WorldPosition position) {
        WorldCell bottomLeft = getCellByPosition(
            Math.max(position.getX() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE),
            Math.max(position.getZ() - WorldSize.INNER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE)
        );
        WorldCell topRight = getCellByPosition(
            Math.min(position.getX() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE),
            Math.min(position.getZ() + WorldSize.INNER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE)
        );

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (int z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                interestArea.add(cells[x][z]);
            }
        }

        return interestArea;
    }

    public List<WorldCell> calculateOuterInterestArea(WorldPosition position, List<WorldCell> innerInterestArea) {
        WorldCell bottomLeft = getCellByPosition(
            Math.max(position.getX() - WorldSize.OUTER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE),
            Math.max(position.getZ() - WorldSize.OUTER_INTEREST_AREA_RADIUS, -WorldSize.MAP_HALFSIZE)
        );
        WorldCell topRight = getCellByPosition(
            Math.min(position.getX() + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE),
            Math.min(position.getZ() + WorldSize.OUTER_INTEREST_AREA_RADIUS, WorldSize.MAP_HALFSIZE)
        );

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = bottomLeft.getX(); x <= topRight.getX(); x++) {
            for (int z = bottomLeft.getZ(); z <= topRight.getZ(); z++) {
                if (!innerInterestArea.contains(cells[x][z])) {
                    interestArea.add(cells[x][z]);
                }
            }
        }

        return interestArea;
    }

    public Location getLocation() {
        return location;
    }


    public boolean isLegalMove(WorldPosition currentPosition, WorldPosition newPosition) {
        float distance = MathUtils.calculateDistance(currentPosition, newPosition);

        if (distance > WorldSize.MAX_SPEED)
            return false;

        return !obstacles.parallelStream()
            .map(obstacle -> obstacle.isPathInsideObstacle(currentPosition, newPosition))
            .filter(result -> result)
            .findAny().orElse(false);
    }

    public void update(long diff) {
        Arrays.asList(cells).parallelStream()
            .map(worldCells -> Arrays.asList(worldCells).parallelStream()
                .map(WorldCell::getSubscribers)
                .flatMap(List::stream)
                .collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .forEach(worldObject -> worldObject.update(diff));
    }

    public void onEvent(WorldObject source, byte eventType, ByteConvertible eventData) {
        Arrays.asList(cells).parallelStream()
            .map(Arrays::asList)
            .flatMap(List::stream)
            .forEach(worldCell -> worldCell.onEvent(source, eventType, eventData));
    }
}
