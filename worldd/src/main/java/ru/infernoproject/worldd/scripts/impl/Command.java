package ru.infernoproject.worldd.scripts.impl;

import org.python.core.PyTuple;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.worldd.characters.CharacterManager;

import java.util.List;

public abstract class Command extends ScriptBase {

    private String level;

    private DataSourceManager dataSourceManager;
    private CharacterManager characterManager;
    private ServerSession session;
    private List<ServerSession> sessions;

    public abstract PyTuple execute(String[] args);

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public void setDataSourceManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }

    public void setCharacterManager(CharacterManager characterManager) {
        this.characterManager = characterManager;
    }

    public CharacterManager getCharacterManager() {
        return characterManager;
    }

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public ServerSession getSession() {
        return session;
    }

    public void setSessions(List<ServerSession> sessions) {
        this.sessions = sessions;
    }

    public List<ServerSession> getSessions() {
        return sessions;
    }
}
