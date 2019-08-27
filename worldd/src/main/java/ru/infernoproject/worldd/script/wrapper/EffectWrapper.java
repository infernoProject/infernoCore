package ru.infernoproject.worldd.script.wrapper;

import ru.infernoproject.worldd.script.impl.EffectBase;
import ru.infernoproject.worldd.script.sql.EffectDirection;
import ru.infernoproject.worldd.script.sql.EffectType;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.object.WorldObject;

public class EffectWrapper {

    private final long id;

    private final EffectBase effect;
    private final EffectType type;
    private final WorldObject caster;

    private long duration;
    private EffectDirection direction;

    public EffectWrapper(EffectBase effect, WorldObject caster, long duration, EffectType type, EffectDirection direction, long id) {
        this.effect = effect;
        this.type = type;
        this.direction = direction;
        this.caster = caster;

        this.duration = duration;
        this.id = id;
    }

    public void process(long diff, WorldCreature target) {
        duration -= diff;
    }

    public long getDuration() {
        return duration;
    }

    public EffectBase getEffect() {
        return effect;
    }

    public WorldObject getCaster() {
        return caster;
    }

    public EffectType getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public void extendDuration(long duration) {
        this.duration += duration;
    }

    public EffectDirection getDirection() {
        return direction;
    }
}
