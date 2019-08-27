package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.script.ScriptHelper;
import ru.infernoproject.worldd.script.impl.EffectBase;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.object.WorldObject;

import javax.script.ScriptException;
import java.util.List;

@SQLObject(database = "objects", table = "spell_effects")
public class Effect implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "duration")
    public long duration;

    @SQLField(column = "type")
    public EffectType type;

    @SQLField(column = "direction")
    public EffectDirection direction;

    @SQLField(column = "script")
    public Script script;

    public void apply(ScriptHelper scriptHelper, WorldObject caster, List<WorldObject> targets) throws ScriptException {
        EffectBase effectBase = (EffectBase) scriptHelper.getScriptManager().eval(script);

        final long duration = ((WorldCreature) caster).processEffects(EffectDirection.OFFENSE, EffectAttribute.DURATION, this.duration);

        targets.parallelStream()
            .filter(target -> WorldCreature.class.isAssignableFrom(target.getClass()))
            .forEach(
                target -> ((WorldCreature) target).applyEffect(effectBase, caster, duration, type, direction, id)
            );
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(name)
            .put(duration)
            .toByteArray();
    }
}
