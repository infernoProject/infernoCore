package ru.infernoproject.worldd.scripts.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.impl.Spell;

import javax.script.ScriptException;

@SQLObject(table = "spells", database = "world")
public class SpellInfo implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "name", type = String.class)
    public String name;

    @SQLField(column = "potency", type = Integer.class)
    public int potency;

    @SQLField(column = "radius", type = Double.class)
    public double radius;

    @SQLField(column = "distance", type = Double.class)
    public double distance;

    @SQLField(column = "cooldown", type = Integer.class)
    public int coolDown;

    @SQLField(column = "script", type = Script.class)
    public Script script;

    public Spell getSpell(ScriptManager scriptManager) throws ScriptException {
        Spell spell = script.toObject(Spell.class, scriptManager.getScriptEngine(), "spell");

        spell.setId(id);
        spell.setName(name);
        spell.setScriptManager(scriptManager);

        spell.setPotency(potency);

        spell.setRadius(radius);
        spell.setDistance(distance);

        spell.setCoolDown(coolDown);

        return spell;
    }
}
