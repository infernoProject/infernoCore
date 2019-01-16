package ru.infernoproject.tests.worldd.map.utils;

import ru.infernoproject.common.utils.ByteArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapGenerator {

    public static void main(String[] args) throws IOException {
        ByteArray mapData = new ByteArray();

        ByteArray obstacle = new ByteArray();
        List<ByteArray> obstaclePoints = new ArrayList<>();

        obstaclePoints.add(new ByteArray().put(.5f).put(-.5f));
        obstaclePoints.add(new ByteArray().put(.5f).put(-1.5f));
        obstaclePoints.add(new ByteArray().put(1.5f).put(-1.5f));
        obstaclePoints.add(new ByteArray().put(1.5f).put(-.5f));
        obstaclePoints.add(new ByteArray().put(.5f).put(-.5f));

        obstacle.put(obstaclePoints);

        mapData.put(Collections.singletonList(obstacle));

        Files.write(Paths.get(args[0]), mapData.toByteArray());
    }
}
