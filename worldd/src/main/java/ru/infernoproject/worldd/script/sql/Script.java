package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.worldd.script.ScriptableObject;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

@SQLObject(database = "objects", table = "scripts")
public class Script implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "script")
    public String script;

    public ScriptableObject invoke(ScriptEngine engine) throws ScriptException {
        engine.eval(script);

        Object result = engine.get("sObject");
        if (!ScriptableObject.class.isAssignableFrom(result.getClass()))
            throw new ScriptException("Script should provide ScriptableObject with name 'sbject'");

        return (ScriptableObject) result;
    }
}
