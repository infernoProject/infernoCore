package ru.infernoproject.worldd.map.utils;

import ru.infernoproject.common.utils.ByteArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MapGenerator {

    public static void main(String[] args) throws IOException {
        ByteArray mapData = new ByteArray();

        mapData.put(new ArrayList<>());

        Files.write(Paths.get(args[0]), mapData.toByteArray());
    }
}
