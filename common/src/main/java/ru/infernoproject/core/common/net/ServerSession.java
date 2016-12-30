package ru.infernoproject.core.common.net;

import java.sql.SQLException;

public interface ServerSession {

    void update();
    void close() throws SQLException;
}
