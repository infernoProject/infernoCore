package ru.infernoproject.common.db.sql;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.utils.HexBin;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public interface SQLObjectWrapper {

    Logger logger = LoggerFactory.getLogger(SQLObjectWrapper.class);

    Map<Class<?>, SQLFieldReader<? extends SQLObjectWrapper>> fieldReaders = new HashMap<Class<?>, SQLFieldReader<? extends SQLObjectWrapper>>() {{
        put(int.class, (d, f, r, o, c) -> f.setInt(o, r.getInt(c)));
        put(long.class, (d, f, r, o, c) -> f.setLong(o, r.getLong(c)));
        put(float.class, (d, f, r, o, c) -> f.setFloat(o, r.getFloat(c)));
        put(double.class, (d, f, r, o, c) -> f.setDouble(o, r.getDouble(c)));
        put(byte[].class, (d, f, r, o, c) -> f.set(o, HexBin.decode(r.getString(c))));
        put(Enum.class, (d, f, r, o, c) -> f.set(o, Enum.valueOf(((Class<? extends Enum>) f.getType()), r.getString(c).toUpperCase())));
        put(String.class, (d, f, r, o, c) -> f.set(o, r.getString(c)));
        put(LocalDateTime.class, (d, f, r, o, c) -> f.set(o, r.getTimestamp(c).toLocalDateTime()));
        put(SQLObjectWrapper.class, (d, f, r, o, c) -> f.set(o, processForeignKey(d, (Class<? extends SQLObjectWrapper>) f.getType(), r.getInt(c))));
    }};

    Map<Class<?>, SQLFieldWriter<? extends SQLObjectWrapper>> fieldWriters = new HashMap<Class<?>, SQLFieldWriter<? extends SQLObjectWrapper>>() {{
        put(int.class, (f, o) -> String.valueOf(f.getInt(o)));
        put(long.class, (f, o) -> String.valueOf(f.getLong(o)));
        put(float.class, (f, o) -> String.valueOf(f.getFloat(o)));
        put(double.class, (f, o) -> String.valueOf(f.getDouble(o)));
        put(byte[].class, (f, o) -> "'" + HexBin.encode((byte[]) f.get(o)) + "'");
        put(Enum.class, (f, o) -> "'" + f.get(o).toString().toLowerCase() + "'");
        put(String.class, (f, o) -> "'" + f.get(o) + "'");
        put(LocalDateTime.class, (f, o) -> "'" + ((LocalDateTime) f.get(o)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'");
        put(SQLObjectWrapper.class, (f, o) -> String.valueOf(getObjectID((Class<? extends SQLObjectWrapper>) f.getType(), (SQLObjectWrapper) f.get(o))));
    }};

    static <T extends SQLObjectWrapper> List<String> listFields(Class<T> objectWrapper) {
        List<String> fieldList = new ArrayList<>();
        for (Field field : objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);
                fieldList.add(String.format("`%s`", sqlField.column()));
            }
            if (field.isAnnotationPresent(SQLFunction.class)) {
                SQLFunction sqlFunction = field.getAnnotation(SQLFunction.class);
                fieldList.add(String.format("%s as `%s`", sqlFunction.expression(), sqlFunction.column()));
            }
        }
        return fieldList;
    }

    static <T extends SQLObjectWrapper> List<String> listFields(Class<T> objectWrapper, boolean skipId) {
        List<String> fieldList = listFields(objectWrapper);

        if (skipId) {
            fieldList.remove("`id`");
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
        List<T> objectList = new ArrayList<>();
        while (resultSet.next()) {
            T object = processObject(dataSourceManager, objectWrapper, resultSet);
            if (object != null)
                objectList.add(object);
        }
        resultSet.close();

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
            if (field.isAnnotationPresent(SQLField.class) || field.isAnnotationPresent(SQLFunction.class)) {
                T.processField(dataSourceManager, object, field, resultSet);
            }
        }

        return object;
    }

    static <T extends SQLObjectWrapper> void processField(DataSourceManager dataSourceManager, T object, Field field, ResultSet resultSet) throws SQLException {
        String column = (field.isAnnotationPresent(SQLField.class)) ? field.getAnnotation(SQLField.class).column() : field.getAnnotation(SQLFunction.class).column();

        try {
            for (Class<?> type : fieldReaders.keySet()) {
                if (type.isAssignableFrom(field.getType())) {
                    ((SQLFieldReader<T>) fieldReaders.get(type)).process(dataSourceManager, field, resultSet, object, column);

                    return;
                }
            }

            logger.warn("Unsupported field type {} for field {}", field.getType().getSimpleName(), field.getName());
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
        List<String> values = new ArrayList<>();
        for (Field field: objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);

                if ("id".equals(sqlField.column()))
                    continue;

                values.add(T.prepareField(field, object));
            }
        }

        return Joiner.on(", ").join(values);
    }

    static  <T extends SQLObjectWrapper> String prepareSet(Class<T> objectWrapper, T object) {
        List<String> values = new ArrayList<>();
        for (Field field: objectWrapper.getDeclaredFields()) {
            if (field.isAnnotationPresent(SQLField.class)) {
                SQLField sqlField = field.getAnnotation(SQLField.class);

                if ("id".equals(sqlField.column()))
                    continue;

                values.add(String.format(
                    "`%s` = %s",
                    sqlField.column(), prepareField(field, object)
                ));
            }
        }

        return Joiner.on(", ").join(values);
    }

    @SuppressWarnings("unchecked")
    static  <T extends SQLObjectWrapper> String prepareField(Field field, T object) {
        try {
            for (Class<?> type : fieldWriters.keySet()) {
                if (type.isAssignableFrom(field.getType())) {
                    return ((SQLFieldWriter<T>) fieldWriters.get(type)).prepare(field, object);
                }
            }

            logger.warn("Unsupported field type {} for field {}", field.getType().getSimpleName(), field.getName());
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
                if ("id".equals(sqlField.column())) {
                    return field.getInt(object);
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("Unable to access ID field of {}: {}", objectWrapper.getSimpleName(), e.getMessage());
        } catch (NoSuchFieldException e) {
            logger.error("{} doesn't have ID field", objectWrapper.getSimpleName());
        }

        return null;
    }
}
