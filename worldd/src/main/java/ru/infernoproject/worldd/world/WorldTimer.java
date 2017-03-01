package ru.infernoproject.worldd.world;

public class WorldTimer {

    private Long currentTime = System.currentTimeMillis();

    public Long tick() {
        Long time = System.currentTimeMillis();
        Long diff = time - currentTime;

        currentTime = time;

        return diff;
    }
}
