package ru.infernoproject.core.common.constants;

public class WorldSize {

    public static final int GRID_MAX_COUNT = 64;

    public static final float GRID_SIZE = 512.0f;

    public static final int CELL_MAX_COUNT = 8;
    public static final float CELL_SIZE = GRID_SIZE / CELL_MAX_COUNT;

    public static final int CELL_TOTAL = GRID_MAX_COUNT * CELL_MAX_COUNT;

    public static final int CENTER_GRID_ID = GRID_MAX_COUNT / 2;
    public static final float CENTER_GRID_OFFSET = GRID_SIZE / 2f;

    public static final int CENTER_CELL_ID = CELL_MAX_COUNT * GRID_MAX_COUNT / 2;
    public static final float CENTER_CELL_OFFSET = CELL_SIZE / 2f;

    public static final float MAP_SIZE = GRID_MAX_COUNT * GRID_SIZE;
    public static final float MAP_HALFSIZE = MAP_SIZE / 2f;
}
