package ru.infernoproject.core.common.types.world;

import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;

public class RaceInfo implements ByteConvertible {

    private int id;
    private String name;
    private String resource;

    public RaceInfo(int id, String name, String resource) {
        this.id = id;
        this.name = name;
        this.resource = resource;
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
