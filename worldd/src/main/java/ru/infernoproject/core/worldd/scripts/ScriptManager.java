package ru.infernoproject.core.worldd.scripts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.worldd.scripts.base.Aura;
import ru.infernoproject.core.worldd.scripts.base.Command;
import ru.infernoproject.core.worldd.scripts.base.Spell;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    private ScriptEngine getScriptEngine() {
        return engineManager.getEngineByName("python");
    }

    public Aura auraGet(int auraId) throws SQLException, ScriptException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            PreparedStatement auraQuery = connection.prepareStatement(
                "SELECT name, potency, tick_interval, duration, script FROM auras WHERE id = ?"
            );

            auraQuery.setInt(1, auraId);

            try (ResultSet resultSet = auraQuery.executeQuery()) {
                if (resultSet.next()) {
                    Script script = new Script(resultSet.getString("script"));
                    Aura aura = script.toObject(Aura.class, getScriptEngine(), "aura");

                    aura.setId(auraId);
                    aura.setName(resultSet.getString("name"));
                    aura.setScriptManager(this);

                    aura.setPotency(resultSet.getInt("potency"));

                    aura.setTickInterval(resultSet.getInt("tick_interval"));
                    aura.setDuration(resultSet.getInt("duration"));

                    return aura;
                }
            }
        }

        return null;
    }

    public Spell spellGet(int spellId) throws SQLException, ScriptException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            PreparedStatement spellQuery = connection.prepareStatement(
                "SELECT name, potency, radius, distance, cooldown, script FROM spells WHERE id = ?"
            );

            spellQuery.setInt(1, spellId);

            try (ResultSet resultSet = spellQuery.executeQuery()) {
                if (resultSet.next()) {
                    Script script = new Script(resultSet.getString("script"));
                    Spell spell = script.toObject(Spell.class, getScriptEngine(), "spell");

                    spell.setId(spellId);
                    spell.setName(resultSet.getString("name"));
                    spell.setScriptManager(this);

                    spell.setPotency(resultSet.getInt("potency"));

                    spell.setRadius(resultSet.getDouble("radius"));
                    spell.setDistance(resultSet.getDouble("distance"));

                    spell.setCoolDown(resultSet.getInt("cooldown"));

                    return spell;
                }
            }
        }

        return null;
    }

    public Command commandGet(String commandName) throws SQLException, ScriptException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            PreparedStatement commandQuery = connection.prepareStatement(
                "SELECT name, level+0 as level, script FROM commands WHERE name = ?"
            );

            commandQuery.setString(1, commandName);

            try (ResultSet resultSet = commandQuery.executeQuery()) {
                if (resultSet.next()) {
                    Script script = new Script(resultSet.getString("script"));

                    Command command = script.toObject(Command.class, getScriptEngine(), "command");

                    command.setName(resultSet.getString("name"));
                    command.setScriptManager(this);

                    command.setLevel(resultSet.getInt("level"));

                    return command;
                }
            }
        }

        return null;
    }

    public boolean spellExists(int spellId) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("world")) {
            PreparedStatement spellExists = connection.prepareStatement(
                "SELECT id FROM spells WHERE id = ?"
            );

            spellExists.setInt(1, spellId);

            try (ResultSet resultSet = spellExists.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
