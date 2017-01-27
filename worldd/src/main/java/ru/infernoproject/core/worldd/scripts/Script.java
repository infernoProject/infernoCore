package ru.infernoproject.core.worldd.scripts;

import ru.infernoproject.core.worldd.scripts.base.Base;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class Script {

    private final String script;

    public Script(String script) {
        this.script = script;
    }

    public <T extends Base> T toObject(Class<T> type, ScriptEngine engine, String instanceName) throws ScriptException {
        engine.eval(script);

        return type.cast(engine.get(instanceName));
    }
}
