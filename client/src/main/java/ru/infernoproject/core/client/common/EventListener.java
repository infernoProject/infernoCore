package ru.infernoproject.core.client.common;

public interface EventListener {

    void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax);
}
