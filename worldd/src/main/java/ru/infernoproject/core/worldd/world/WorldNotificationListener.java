package ru.infernoproject.core.worldd.world;

public interface WorldNotificationListener {

    void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax);

}
