package ru.infernoproject.core.worldd.scripts.impl;

import ru.infernoproject.core.worldd.scripts.base.Base;
import ru.infernoproject.core.worldd.world.creature.WorldCreature;

public abstract class Spell extends Base {

    private int potency;
    private double radius;
    private double distance;
    private int coolDown;

    public abstract void cast(WorldCreature source, WorldCreature[] target);

    public int getPotency() {
        return potency;
    }

    public void setPotency(int potency) {
        this.potency = potency;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }
}
