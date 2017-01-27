package ru.infernoproject.core.worldd.scripts.base;

import org.python.core.PyTuple;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.net.server.ServerSession;
import ru.infernoproject.core.worldd.characters.CharacterManager;

import java.util.List;

public abstract class Command extends Base {

    private int level;
    private DataSourceManager dataSourceManager;
    private CharacterManager characterManager;
    private ServerSession session;
    private List<ServerSession> sessions;

    public abstract PyTuple execute(String[] args);

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
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
