package ru.infernoproject.worldd.scripts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;

import ru.infernoproject.worldd.scripts.sql.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.SQLException;

public class ScriptManager {

    private final DataSourceManager dataSourceManager;

    private static final ScriptEngineManager engineManager = new ScriptEngineManager();
    private static final Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    public ScriptManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

        try {
            getScriptEngine().eval("isNull = lambda x: x is None");
            logger.info("ScriptEngine initialized");
        } catch (ScriptException e) {
            logger.error("Unable to initialize ScriptEngine: {}", e.getMessage());
        }
    }

    public ScriptEngine getScriptEngine() {
        return engineManager.getEngineByName("python");
    }

    public AuraInfo auraGet(int auraId) throws SQLException, ScriptException {
        return dataSourceManager.query(AuraInfo.class).select()
            .filter(new SQLFilter("id").eq(auraId))
            .fetchOne();
    }

    public SpellInfo spellGet(int spellId) throws SQLException, ScriptException {
        return dataSourceManager.query(SpellInfo.class).select()
            .filter(new SQLFilter("id").eq(spellId))
            .fetchOne();

    }

    public CommandInfo commandGet(String commandName) throws SQLException, ScriptException {
        return  dataSourceManager.query(CommandInfo.class).select()
            .filter(new SQLFilter("name").eq(commandName))
            .fetchOne();
    }
}
