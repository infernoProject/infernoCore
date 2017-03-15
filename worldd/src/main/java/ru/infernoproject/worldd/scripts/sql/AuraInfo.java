package ru.infernoproject.worldd.scripts.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.impl.Aura;

import javax.script.ScriptException;

@SQLObject(table = "auras", database = "world")
public class AuraInfo implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "potency")
    public int potency;

    @SQLField(column = "tick_interval")
    public int tickInterval;

    @SQLField(column = "duration")
    public int duration;

    @SQLField(column = "script")
    public Script script;

    public Aura getAura(ScriptManager scriptManager) throws ScriptException {
        Aura aura = script.toObject(Aura.class, scriptManager.getScriptEngine(), "aura");

        aura.setId(id);
        aura.setName(name);
        aura.setScriptManager(scriptManager);

        aura.setPotency(potency);

        aura.setTickInterval(tickInterval);
        aura.setDuration(duration);

        return aura;
    }
}
