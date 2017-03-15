package ru.infernoproject.worldd.data.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

@SQLObject(table = "races", database = "world")
public class RaceInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "resource")
    public String resource;

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(name).put(resource)
            .toByteArray();
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }
}
