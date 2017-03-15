package ru.infernoproject.worldd.scripts.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.worldd.scripts.impl.ScriptBase;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

@SQLObject(table = "scripts", database = "world")
public class Script implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "type")
    public int type;

    @SQLField(column = "script")
    public String scriptData;

    public <T extends ScriptBase> T toObject(Class<T> type, ScriptEngine engine, String instanceName) throws ScriptException {
        engine.eval(scriptData);

        return type.cast(engine.get(instanceName));
    }
}
