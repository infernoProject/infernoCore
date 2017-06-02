package ru.infernoproject.common.db.sql;

import ru.infernoproject.common.db.DataSourceManager;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface SQLFieldReader<T extends SQLObjectWrapper> {

    void process(DataSourceManager dataSourceManager, Field field, ResultSet resultSet, T object, String column) throws SQLException, IllegalAccessException;

}
