package ru.linachan.inferno.world.interest;

import ru.linachan.yggdrasil.common.vector.Vector2;

import java.util.UUID;

public interface InterestObject {

    String getName();
    UUID getID();
    Vector2<Double, Double> getPosition();
}
