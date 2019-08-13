package ru.infernoproject.worldd.world;

public class WorldTimer {

    private Long currentTime = System.currentTimeMillis();

    private Long serverTime = 0L;
    private Integer serverTimeRate = 10;

    public static final WorldTimer WORLD_TIMER = new WorldTimer();

    public Long tick() {
        Long time = System.currentTimeMillis();
        Long diff = time - currentTime;

        serverTime = (serverTime + diff * serverTimeRate) % (86400 * 1000);

        currentTime = time;

        return diff;
    }

    public Long getServerTime() {
        return serverTime;
    }

    public int getServerTimeRate() {
        return serverTimeRate;
    }

    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }

    public void setServerTimeRate(int serverTimeRate) {
        this.serverTimeRate = serverTimeRate;
    }
}
