package ru.infernoproject.common.data.sql;

import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

@SQLObject(table = "classes", database = "objects")
public class ClassInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "resource")
    public String resource;

    public int getId() {
        return id;
    }

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
}
