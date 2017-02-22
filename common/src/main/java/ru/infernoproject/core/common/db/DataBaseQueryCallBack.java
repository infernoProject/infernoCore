package ru.infernoproject.core.common.db;

import java.sql.ResultSet;

public interface DataBaseQueryCallBack {

    Object processResult(ResultSet resultSet) throws Exception;
}
