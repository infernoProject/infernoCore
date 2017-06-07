package ru.infernoproject.worldd.script;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.sql.SQLException;
import java.util.List;

public class ScriptManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptEngineFactory scriptEngineFactory = new NashornScriptEngineFactory();

    public ScriptManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public ScriptableObject invokeScript(Script script) throws ScriptException {
        return script.invoke(scriptEngineFactory.getScriptEngine());
    }

    public ScriptValidationResult validateScript(Script script) {
        try {
            ScriptableObject object = script.invoke(scriptEngineFactory.getScriptEngine());

            if (object == null)
                return new ScriptValidationResult("Script should define ScriptableObject with name 'sObject'");
        } catch (ScriptException e) {
            return new ScriptValidationResult(e);
        }

        return new ScriptValidationResult();
    }

    public List<Script> listScripts() throws SQLException {
        return dataSourceManager.query(Script.class).select()
            .fetchAll();
    }

    public Script getScript(int id) throws SQLException {
        return dataSourceManager.query(Script.class).select()
            .filter(new SQLFilter("id").eq(id))
            .fetchOne();
    }

    public ScriptValidationResult updateScript(int id, String script) throws SQLException {
        Script scriptData = getScript(id);

        if (scriptData != null) {
            scriptData.script = script;

            ScriptValidationResult result = validateScript(scriptData);
            if (result.isValid()) {
                dataSourceManager.query(Script.class).update(scriptData);
            }

            return result;
        }

        return new ScriptValidationResult("Script doesn't exist");
    }

    public Command getCommand(String command) throws SQLException {
        return dataSourceManager.query(Command.class).select()
            .filter(new SQLFilter("name").eq(command))
            .fetchOne();
    }
}
