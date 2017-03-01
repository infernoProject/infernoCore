package ru.infernoproject.common.db.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public interface SQLQuery<T extends SQLObjectWrapper> {

    Logger logger = LoggerFactory.getLogger(SQLQuery.class);

    String prepareQuery();

    default List<T> fetchAll() throws SQLException {
        throw new UnsupportedOperationException();
    }

    default T fetchOne() throws SQLException {
        throw new UnsupportedOperationException();
    }

    default Integer execute() throws SQLException  {
        throw new UnsupportedOperationException();
    }

    default Integer executeRaw(String query) throws SQLException  {
        throw new UnsupportedOperationException();
    }

}