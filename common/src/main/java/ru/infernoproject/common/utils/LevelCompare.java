package ru.infernoproject.common.utils;

import java.util.HashMap;
import java.util.Map;

public class LevelCompare {

    private static final Map<String, Integer> LEVELS = new HashMap<>();

    static {
        LEVELS.put("user", 1);
        LEVELS.put("moderator", 2);
        LEVELS.put("game_master", 3);
        LEVELS.put("admin", 4);
    }

    public static boolean isAdmin(String level) {
        return LEVELS.getOrDefault(level, 1) > LEVELS.get("admin");
    }

    public static boolean checkLevel(String level, int minLevel) {
        return LEVELS.getOrDefault(level, 1) > minLevel;
    }

    public static int toInteger(String level) {
        return LEVELS.getOrDefault(level, 1);
    }
}
