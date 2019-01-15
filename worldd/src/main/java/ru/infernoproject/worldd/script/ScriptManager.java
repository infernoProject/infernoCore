package ru.infernoproject.worldd.script;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.script.sql.Command;
import ru.infernoproject.worldd.script.sql.Script;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScriptManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptEngineManager engineManager = new ScriptEngineManager();

    public ScriptManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public ScriptableObject eval(Script script) throws ScriptException {
        ScriptEngine engine = getScriptEngineForLanguage(script.language);

        engine.eval(script.script);

        Object result = engine.get("sObject");
        if ((result == null) || !ScriptableObject.class.isAssignableFrom(result.getClass()))
            throw new ScriptException("Script should provide ScriptableObject with name 'sObject'");

        return (ScriptableObject) result;
    }

    private ScriptEngine getScriptEngineForLanguage(String language) {
        Optional<ScriptEngineFactory> engineFactoryOptional = engineManager.getEngineFactories().stream()
            .filter(factory -> factory.getLanguageName().equals(language))
            .findFirst();

        if (!engineFactoryOptional.isPresent()) {
            throw new IllegalStateException(String.format(
                "No engines available for language: %s", language
            ));
        }

        return engineFactoryOptional.get().getScriptEngine();
    }

    public ScriptValidationResult validateScript(Script script) {
        try {
            ScriptableObject object = eval(script);

            if (object == null)
                return new ScriptValidationResult("Script should define ScriptableObject with name 'sObject'");
        } catch (ScriptException e) {
            return new ScriptValidationResult(e);
        }

        return new ScriptValidationResult();
    }

    public List<String> getAvailableLanguages() {
        return engineManager.getEngineFactories().stream()
            .map(ScriptEngineFactory::getLanguageName)
            .collect(Collectors.toList());
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

    public ScriptValidationResult updateScript(int id, String lang, String script) throws SQLException {
        if (!getAvailableLanguages().contains(lang))
            return new ScriptValidationResult("Script language is not supported");

        Script scriptData = getScript(id);

        if (scriptData != null) {
            scriptData.language = lang;
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
