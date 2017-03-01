package ru.infernoproject.worldd.scripts.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.impl.Aura;

import javax.script.ScriptException;

@SQLObject(table = "auras", database = "world")
public class AuraInfo implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "name", type = String.class)
    public String name;

    @SQLField(column = "potency", type = Integer.class)
    public int potency;

    @SQLField(column = "tick_interval", type = Integer.class)
    public int tickInterval;

    @SQLField(column = "duration", type = Integer.class)
    public int duration;

    @SQLField(column = "script", type = Script.class)
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
