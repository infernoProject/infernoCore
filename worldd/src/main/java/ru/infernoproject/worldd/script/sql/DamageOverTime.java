package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.script.ScriptHelper;
import ru.infernoproject.worldd.script.impl.DamageOverTimeBase;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.object.WorldObject;

import javax.script.ScriptException;
import java.util.List;

@SQLObject(database = "objects", table = "spell_damage_over_time_effects")
public class DamageOverTime implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "basic_potential")
    public long basicPotential;

    @SQLField(column = "tick_interval")
    public long tickInterval;

    @SQLField(column = "duration")
    public long duration;

    @SQLField(column = "script")
    public Script script;

    public void apply(ScriptHelper scriptHelper, WorldObject caster, List<WorldObject> targets) throws ScriptException {
        DamageOverTimeBase dotBase = (DamageOverTimeBase) scriptHelper.getScriptManager().eval(script);

        targets.parallelStream()
            .filter(target -> WorldCreature.class.isAssignableFrom(target.getClass()))
            .forEach(
                target -> ((WorldCreature) target).applyDamageOverTime(dotBase, caster, duration, tickInterval, basicPotential, id)
            );
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(name)
            .put(basicPotential)
            .put(tickInterval).put(duration)
            .toByteArray();
    }
}
