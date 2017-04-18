package ru.infernoproject.common.server;

import java.sql.SQLException;

public interface ServerJob {

    void run() throws SQLException;
}
