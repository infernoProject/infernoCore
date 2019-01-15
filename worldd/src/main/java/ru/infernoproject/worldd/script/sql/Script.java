package ru.infernoproject.worldd.script.sql;

import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;

@SQLObject(database = "objects", table = "scripts")
public class Script implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "script")
    public String script;

    @SQLField(column = "lang")
    public String language;
}
