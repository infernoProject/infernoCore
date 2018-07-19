package ru.infernoproject.worldd.map;

import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.util.ArrayList;
import java.util.List;

public class WorldMap {

    private final Location location;
    private final WorldCell[][] cells = new WorldCell[WorldSize.CELL_TOTAL][WorldSize.CELL_TOTAL];

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
        int x = (int) Math.floor(position.getX() / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID;
        int y = (int) Math.floor(position.getY() / WorldSize.CELL_SIZE) + WorldSize.CENTER_CELL_ID;

        return cells[x][y];
    }

    public List<WorldCell> calculateInnerInterestArea(WorldCell center, int innerRadius) {
        int firstX = Math.max(center.getX() - innerRadius, 0);
        int lastX = Math.min(center.getX() + innerRadius, WorldSize.CELL_TOTAL - 1);

        int firstY = Math.max(center.getY() - innerRadius, 0);
        int lastY = Math.min(center.getY() + innerRadius, WorldSize.CELL_TOTAL - 1);

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                interestArea.add(cells[x][y]);
            }
        }

        return interestArea;
    }

    public List<WorldCell> calculateOuterInterestArea(WorldCell center, int innerRadius, int outerRadius) {
        int firstX = Math.max(center.getX() - outerRadius, 0);
        int lastX = Math.min(center.getX() + outerRadius, WorldSize.CELL_TOTAL - 1);

        int firstY = Math.max(center.getY() - outerRadius, 0);
        int lastY = Math.min(center.getY() + outerRadius, WorldSize.CELL_TOTAL - 1);

        int firstInnerX = Math.max(center.getX() - innerRadius, firstX);
        int lastInnerX = Math.min(center.getX() + innerRadius, lastX);

        int firstInnerY = Math.max(center.getY() - innerRadius, firstY);
        int lastInnerY = Math.min(center.getY() + innerRadius, lastY);

        List<WorldCell> interestArea = new ArrayList<>();

        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                if (x < firstInnerX || x > lastInnerX || y < firstInnerY || y > lastInnerY) {
                    interestArea.add(cells[x][y]);
                }
            }
        }

        return interestArea;
    }

    public Location getLocation() {
        return location;
    }
}
