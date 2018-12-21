package ru.infernoproject.worldd.map;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.map.sql.Location;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class WorldMapManager {

    private final DataSourceManager dataSourceManager;
    private final Map<Integer, WorldMap> maps = new HashMap<>();

    public WorldMapManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public void update(Long diff) {
        maps.values().parallelStream()
            .forEach(map -> map.update(diff));
    }

    public WorldMap getMap(WorldPosition position) {
        WorldMap worldMap = maps.get(position.getLocation());
        if (worldMap == null)
            throw new IllegalStateException(String.format(
                "MapData for location ID%d doesn't exist", position.getLocation()
            ));

        return worldMap;
    }

    public void readMapData(File mapDataPath) throws SQLException {
        if (!mapDataPath.exists())
            throw new IllegalStateException("MapData folder doesn't exist");

        this.dataSourceManager.query(Location.class).select()
            .fetchAll().forEach(location -> {
                try {
                    readMapData(mapDataPath, location);
                } catch (IOException e) {
                    throw new IllegalStateException(String.format(
                        "Unable to read MapData file '%s': %s", location.name, e.getMessage()
                    ));
                }
            });
    }

    private void readMapData(File mapDataPath, Location location) throws IOException {
        File mapDataFile = new File(mapDataPath, String.format("%s.map", location.name));
        if (mapDataFile.exists()) {
            ByteWrapper mapData = ByteWrapper.readFile(mapDataFile);
            WorldMap worldMap = new WorldMap(location, mapData);

            maps.put(location.id, worldMap);
        } else {
            throw new IllegalStateException(String.format("MapData file for '%s' doesn't exist", location.name));
        }
    }
}
