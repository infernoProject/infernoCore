package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteArray;

import ru.infernoproject.worldd.script.ScriptManager;
import ru.infernoproject.worldd.script.impl.CommandBase;

import javax.script.ScriptException;

@SQLObject(database = "objects", table = "commands")
public class Command implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "level")
    public AccountLevel level;

    @SQLField(column = "script")
    public Script script;

    public ByteArray execute(ScriptManager scriptManager, DataSourceManager dataSourceManager, Session session, String[] args) throws ScriptException {
        CommandBase commandBase = (CommandBase) scriptManager.invokeScript(script);

        return commandBase.execute(dataSourceManager, session,  args);
    }
}
