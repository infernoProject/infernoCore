package ru.infernoproject.core.common.types.world;

import ru.infernoproject.core.common.db.sql.SQLField;
import ru.infernoproject.core.common.db.sql.SQLObject;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;

@SQLObject(table = "races", database = "world")
public class RaceInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "name", type = String.class)
    public String name;

    @SQLField(column = "resource", type = String.class)
    public String resource;

    public RaceInfo() {

    }

    public RaceInfo(ByteWrapper data) {
        id = data.getInt();
        name = data.getString();
        resource = data.getString();
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

    public int getId() {
        return id;
    }
}
