package ru.infernoproject.core.common.db.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public interface SQLQuery<T extends SQLObjectWrapper> {

    Logger logger = LoggerFactory.getLogger(SQLQuery.class);

    String prepareQuery();

    List<T> fetchAll() throws SQLException;
    T fetchOne() throws SQLException;

    Integer execute() throws SQLException;
    Integer executeRaw(String query) throws SQLException;

}