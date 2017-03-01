package ru.infernoproject.common.db.sql;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SQLObject {

    String database();
    String table();
}
