package ru.infernoproject.core.common.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface DataBaseQueryConfigurer {

    void configure(PreparedStatement statement) throws SQLException;
}
