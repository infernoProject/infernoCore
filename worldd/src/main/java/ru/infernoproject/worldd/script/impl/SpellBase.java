package ru.infernoproject.worldd.script.impl;

import ru.infernoproject.worldd.script.ScriptHelper;
import ru.infernoproject.worldd.script.ScriptableObject;
import ru.infernoproject.worldd.world.object.WorldObject;

@FunctionalInterface
public interface SpellBase extends ScriptableObject {

    void cast(ScriptHelper scriptHelper, WorldObject caster, WorldObject target, long basicPotential);
}
