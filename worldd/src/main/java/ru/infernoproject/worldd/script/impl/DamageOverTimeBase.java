package ru.infernoproject.worldd.script.impl;

import ru.infernoproject.worldd.script.ScriptableObject;
import ru.infernoproject.worldd.world.object.WorldObject;

@FunctionalInterface
public interface DamageOverTimeBase extends ScriptableObject {

    void tick(WorldObject caster, WorldObject target, long basicPotential);
}
