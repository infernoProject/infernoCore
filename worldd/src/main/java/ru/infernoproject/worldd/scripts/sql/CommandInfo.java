package ru.infernoproject.worldd.scripts.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.impl.Command;

import javax.script.ScriptException;

@SQLObject(table = "commands", database = "world")
public class CommandInfo implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "name", type = String.class)
    public String name;

    @SQLField(column = "level", type = String.class)
    public String level;

    @SQLField(column = "script", type = Script.class)
    public Script script;

    public Command getCommand(ScriptManager scriptManager) throws ScriptException {
        Command command = script.toObject(Command.class, scriptManager.getScriptEngine(), "command");

        command.setName(name);
        command.setScriptManager(scriptManager);

        command.setLevel(level);

        return command;
    }

}