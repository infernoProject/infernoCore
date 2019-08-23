package ru.infernoproject.worldd.script.impl;

import ru.infernoproject.worldd.script.ScriptableObject;
import ru.infernoproject.worldd.world.object.WorldObject;

public interface EffectBase extends ScriptableObject {

    long processPotential(long basicPotential);
    long processDuration(long duration);
    long processTickTime(long tickTime);
    long processCoolDown(long coolDown);
    long processCastTime(long castTime);

}
