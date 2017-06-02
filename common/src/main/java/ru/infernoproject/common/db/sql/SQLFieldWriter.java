package ru.infernoproject.common.db.sql;

import java.lang.reflect.Field;

public interface SQLFieldWriter<T extends SQLObjectWrapper> {

    String prepare(Field field, T object) throws IllegalAccessException;
}
