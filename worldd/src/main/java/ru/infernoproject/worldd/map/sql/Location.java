package ru.infernoproject.worldd.map.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

@SQLObject(database = "objects", table = "locations")
public class Location implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;
}
