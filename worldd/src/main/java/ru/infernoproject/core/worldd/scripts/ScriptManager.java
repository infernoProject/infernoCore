package ru.infernoproject.core.worldd.scripts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.error.CoreException;
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

    public Aura auraGet(int auraId) throws CoreException {
        return (Aura) dataSourceManager.query(
            "world", "SELECT name, potency, tick_interval, duration, script FROM auras WHERE id = ?"
        ).configure(query -> {
            query.setInt(1, auraId);
        }).executeSelect(resultSet -> {
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

            return null;
        });
    }

    public Spell spellGet(int spellId) throws CoreException {
        return (Spell) dataSourceManager.query(
            "world", "SELECT name, potency, radius, distance, cooldown, script FROM spells WHERE id = ?"
        ).configure(query -> {
            query.setInt(1, spellId);
        }).executeSelect(resultSet -> {
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

            return null;
        });
    }

    public Boolean spellExists(int spellId) throws CoreException {
        return (Boolean) dataSourceManager.query(
            "world", "SELECT id FROM spells WHERE id = ?"
        ).configure(query -> {
            query.setInt(1, spellId);
        }).executeSelect(ResultSet::next);
    }

    public Command commandGet(String commandName) throws CoreException {
        return (Command) dataSourceManager.query(
            "world", "SELECT name, level+0 as level, script FROM commands WHERE name = ?"
        ).configure(query -> {
            query.setString(1, commandName);
        }).executeSelect(resultSet -> {
            if (resultSet.next()) {
                Script script = new Script(resultSet.getString("script"));

                Command command = script.toObject(Command.class, getScriptEngine(), "command");

                command.setName(resultSet.getString("name"));
                command.setScriptManager(this);

                command.setLevel(resultSet.getInt("level"));

                return command;
            }

            return null;
        });
    }
}
