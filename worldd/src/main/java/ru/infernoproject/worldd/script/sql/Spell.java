package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.script.ScriptManager;
import ru.infernoproject.worldd.script.impl.SpellBase;
import ru.infernoproject.worldd.world.object.WorldObject;

import javax.script.ScriptException;
import java.util.List;

@SQLObject(database = "objects", table = "spells")
public class Spell implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "type")
    public SpellType type;

    @SQLField(column = "required_class")
    public ClassInfo requiredClass;

    @SQLField(column = "required_level")
    public long requiredLevel;

    @SQLField(column = "cool_down")
    public long coolDown;

    @SQLField(column = "distance")
    public float distance;

    @SQLField(column = "radius")
    public float radius;

    @SQLField(column = "basic_potential")
    public long basicPotential;

    @SQLField(column = "script")
    public Script script;

    public void cast(ScriptManager scriptManager, WorldObject caster, List<WorldObject> targets) throws ScriptException {
        SpellBase spellBase = (SpellBase) scriptManager.eval(script);

        targets.parallelStream().forEach(
            target -> spellBase.cast(caster, target, basicPotential)
        );

        caster.addCoolDown(id, coolDown);
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(name).put(type.toString().toLowerCase())
            .put(distance).put(radius).put(basicPotential)
            .put(coolDown)
            .toByteArray();
    }
}
