package ru.infernoproject.worldd.script;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.sql.SQLException;

public class ScriptManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptEngineFactory scriptEngineFactory = new NashornScriptEngineFactory();

    public ScriptManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public ScriptableObject invokeScript(Script script, String targetObject) throws ScriptException {
        return script.invoke(scriptEngineFactory.getScriptEngine(), targetObject);
    }

    public Command getCommand(String command) throws SQLException {
        return dataSourceManager.query(Command.class).select()
            .filter(new SQLFilter("name").eq(command))
            .fetchOne();
    }
}
