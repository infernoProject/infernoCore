package ru.infernoproject.worldd.scripts.impl;

import ru.infernoproject.worldd.scripts.ScriptManager;

public class ScriptBase {

    private int id;
    private String name;
    private ScriptManager scriptManager;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public void setScriptManager(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }
}
