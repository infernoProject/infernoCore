package ru.infernoproject.core.common.db;

import java.sql.PreparedStatement;

public interface DataBaseQueryConfigurer {

    void configure(PreparedStatement query) throws Exception;
}
