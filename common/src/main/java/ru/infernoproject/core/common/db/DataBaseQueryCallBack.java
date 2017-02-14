package ru.infernoproject.core.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface DataBaseQueryCallBack {

    Object processResult(ResultSet result) throws SQLException;
}
