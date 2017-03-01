package ru.infernoproject.common.db.sql;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.db.DataSourceManager;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface SQLObjectWrapper {

    Logger logger = LoggerFactory.getLogger(SQLObjectWrapper.class);

    static <T extends SQLObjectWrapper> List<String> listFields(Class<T> objectWrapper) {
        List<String> fieldList = new ArrayList<>();
        for (Field field : objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);
                fieldList.add(sqlField.column());
            }
        }
        return fieldList;
    }

    static <T extends SQLObjectWrapper> List<String> listFields(Class<T> objectWrapper, boolean skipId) {
        List<String> fieldList = listFields(objectWrapper);

        if (skipId) {
            fieldList.remove("id");
        }

        return fieldList;
    }

    static <T extends SQLObjectWrapper> String getTableName(Class<T> objectWrapper) {
        return getSQLObjectInfo(objectWrapper).table();
    }

    static <T extends SQLObjectWrapper> String getDataBaseName(Class<T> objectWrapper) {
        return getSQLObjectInfo(objectWrapper).database();
    }

    static <T extends SQLObjectWrapper> List<T> processResultSet(DataSourceManager dataSourceManager, Class<T> objectWrapper, ResultSet resultSet) throws SQLException {
        SQLObject sqlObjectInfo = getSQLObjectInfo(objectWrapper);

        List<T> objectList = new ArrayList<>();
        while (resultSet.next()) {
            T object = processObject(dataSourceManager, objectWrapper, resultSet);
            if (object != null)
                objectList.add(object);
        }
        return objectList;
    }

    static <T extends SQLObjectWrapper> T processObject(DataSourceManager dataSourceManager, Class<T> objectWrapper, ResultSet resultSet) throws SQLException {
        try {
            T object = objectWrapper.newInstance();

            return processObject(dataSourceManager, objectWrapper, resultSet, object);
        } catch (InstantiationException e) {
            logger.error("Unable to instantiate {}: {}", objectWrapper.getSimpleName(), e.getMessage());
        } catch (IllegalAccessException e) {
            logger.error("Unable to access {}: {}", objectWrapper.getSimpleName(), e.getMessage());
        }

        return null;
    }

    static <T extends SQLObjectWrapper> T processObject(DataSourceManager dataSourceManager, Class<T> objectWrapper, ResultSet resultSet, T object) throws SQLException {
        for (Field field : objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                T.processField(dataSourceManager, object, field, resultSet);
            }
        }
        return object;
    }

    static <T extends SQLObjectWrapper> void processField(DataSourceManager dataSourceManager, T object, Field field, ResultSet resultSet) throws SQLException {
        SQLField fieldInfo = field.getAnnotation(SQLField.class);

        try {
            if (Integer.class.isAssignableFrom(fieldInfo.type())) {
                field.setInt(object, resultSet.getInt(fieldInfo.column()));
            } else if (Long.class.isAssignableFrom(fieldInfo.type())) {
                field.setLong(object, resultSet.getLong(fieldInfo.column()));
            } else if (Float.class.isAssignableFrom(fieldInfo.type())) {
                field.setFloat(object, resultSet.getFloat(fieldInfo.column()));
            } else if (Double.class.isAssignableFrom(fieldInfo.type())) {
                field.setDouble(object, resultSet.getDouble(fieldInfo.column()));
            } else if (String.class.isAssignableFrom(fieldInfo.type())) {
                field.set(object, resultSet.getString(fieldInfo.column()));
            } else if (SQLObjectWrapper.class.isAssignableFrom(fieldInfo.type())) {
                field.set(object, T.processForeignKey(
                    dataSourceManager, (Class<? extends SQLObjectWrapper>) fieldInfo.type(),
                    resultSet.getInt(fieldInfo.column())
                ));
            } else {
                logger.warn("Unsupported field type {} for field {}", fieldInfo.type().getSimpleName(), field.getName());
            }
        } catch (IllegalAccessException e) {
            logger.error("Unable to access {}: {}", object.getClass().getSimpleName(), e.getMessage());
        }

    }

    static  <O extends SQLObjectWrapper> O processForeignKey(DataSourceManager dataSourceManager, Class<O> objectWrapper, int objectId) throws SQLException {
        return dataSourceManager.query(objectWrapper).select()
            .filter(new SQLFilter("id").eq(objectId))
            .fetchOne();
    }

    static  <T extends SQLObjectWrapper> String prepareValue(Class<T> objectWrapper, T object) {
        SQLObject sqlObject = getSQLObjectInfo(objectWrapper);

        List<String> values = new ArrayList<>();
        for (Field field: objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);

                if (sqlField.column().equals("id"))
                    continue;

                values.add(T.prepareField(field, object));
            }
        }
        return Joiner.on(", ").join(values);
    }

    static  <T extends SQLObjectWrapper> String prepareSet(Class<T> objectWrapper, T object) {
        SQLObject sqlObject = getSQLObjectInfo(objectWrapper);

        List<String> values = new ArrayList<>();
        for (Field field: objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);

                if (sqlField.column().equals("id"))
                    continue;

                values.add(String.format(
                    "`%s` = %s",
                    sqlField.column(), prepareField(field, object)
                ));
            }
        }
        return Joiner.on(", ").join(values);
    }

    static  <T extends SQLObjectWrapper> String prepareField(Field field, T object) {
        SQLField fieldInfo = field.getAnnotation(SQLField.class);

        try {
            if (fieldInfo.type().equals(String.class)) {
                return "'" + field.get(object) + "'";
            } else if (fieldInfo.type().equals(Integer.class)) {
                return String.valueOf(field.getInt(object));
            } else if (fieldInfo.type().equals(Long.class)) {
                return String.valueOf(field.getLong(object));
            } else if (fieldInfo.type().equals(Float.class)) {
                return String.valueOf(field.getFloat(object));
            } else if (fieldInfo.type().equals(Double.class)) {
                return String.valueOf(field.getDouble(object));
            } else if (SQLObjectWrapper.class.isAssignableFrom(fieldInfo.type())) {
                return String.valueOf(getObjectID(
                    (Class<? extends SQLObjectWrapper>) fieldInfo.type(),
                    (SQLObjectWrapper) field.get(object))
                );
            } else {
                logger.warn("Unsupported field type {} for field {}", fieldInfo.type().getSimpleName(), field.getName());
            }
        } catch (IllegalAccessException e) {
            logger.error("Unable to access field {}: {}", field.getName(), e.getMessage());
        }

        return "";
    }

    static  <T extends SQLObjectWrapper> SQLObject getSQLObjectInfo(Class<T> objectWrapper) {
        if (!objectWrapper.isAnnotationPresent(SQLObject.class)) {
            throw new IllegalArgumentException(String.format("%s is not SQLObject", objectWrapper.getSimpleName()));
        }

        return objectWrapper.getAnnotation(SQLObject.class);
    }

    static  <O extends SQLObjectWrapper> Integer getObjectID(Class<O> objectWrapper, SQLObjectWrapper object) {
        if (object == null)
            return null;

        try {
            Field field = objectWrapper.getDeclaredField("id");
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);
                if (sqlField.column().equals("id")) {
                    return field.getInt(object);
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("Unable to access ID field of {}: {}", objectWrapper.getSimpleName(), e.getMessage());
        } catch (NoSuchFieldException e) {
            logger.error("{} doesn't have ID field", objectWrapper.getSimpleName());
        }

        return 0;
    }
}
