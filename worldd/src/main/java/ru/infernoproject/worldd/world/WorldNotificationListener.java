package ru.infernoproject.worldd.world;

public interface WorldNotificationListener {

    void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax);

}
