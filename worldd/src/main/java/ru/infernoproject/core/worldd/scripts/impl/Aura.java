package ru.infernoproject.core.worldd.scripts.impl;

import ru.infernoproject.core.worldd.scripts.base.Base;
import ru.infernoproject.core.worldd.world.creature.WorldCreature;

public abstract class Aura extends Base {

    private int potency;
    private int tickInterval;
    private int duration;
    private WorldCreature caster;

    private int nextTick;

    public void process(Long diff, WorldCreature target) {
        duration -= diff;

        if ((duration > 0)&&(Math.abs(duration - nextTick) > tickInterval)) {
            tick(caster, target);
        }
    }

    protected abstract void tick(WorldCreature caster, WorldCreature target);

    public int getPotency() {
        return potency;
    }

    public void setPotency(int baseDamage) {
        this.potency = baseDamage;
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        this.nextTick = duration;
    }

    public void extendDuration(int duration) {
        this.duration += duration;
    }

    public void setCaster(WorldCreature caster) {
        this.caster = caster;
    }
}
