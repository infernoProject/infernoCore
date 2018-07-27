package ru.infernoproject.worldd.script.impl;

import ru.infernoproject.worldd.script.ScriptableObject;
import ru.infernoproject.worldd.world.object.WorldObject;

public interface SpellBase extends ScriptableObject {

    void cast(WorldObject caster, WorldObject target, long basicPotential);
}
