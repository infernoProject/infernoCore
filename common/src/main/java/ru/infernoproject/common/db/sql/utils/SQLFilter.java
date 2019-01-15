package ru.infernoproject.common.db.sql.utils;

import com.google.common.base.Joiner;

import java.util.List;

public final class SQLFilter {

    private String column;
    private String filter;

    public SQLFilter(String column) {
        this.column = column;
    }

    public SQLFilter(SQLFilter sqlFilter) {
        this.column = sqlFilter.column;
        this.filter = sqlFilter.filter;
    }

    public SQLFilter() {
        // Default constructor
    }

    public SQLFilter like(String pattern) {
        return or(
            new SQLFilter(this).raw(String.format("`%s` LIKE '%%%s%%'", column, pattern)),
            new SQLFilter(this).raw(String.format("`%s` LIKE '%s%%'", column, pattern)),
            new SQLFilter(this).raw(String.format("`%s` LIKE '%%%s'", column, pattern)),
            new SQLFilter(this).raw(String.format("`%s` LIKE '%s'", column, pattern))
        );
    }

    public SQLFilter eq(String value) {
        return raw(String.format("`%s` = '%s'", column, value));
    }

    public SQLFilter eq(Integer value) {
        return raw(String.format("`%s` = %s", column, value));
    }

    public SQLFilter lt(Integer value) {
        return raw(String.format("`%s` < %s", column, value));
    }

    public SQLFilter le(Integer value) {
        return raw(String.format("`%s` <= %s", column, value));
    }

    public SQLFilter gt(Integer value) {
        return raw(String.format("`%s` > %s", column, value));
    }

    public SQLFilter ge(Integer value) {
        return raw(String.format("`%s` >= %s", column, value));
    }

    public SQLFilter ne(Integer value) {
        return raw(String.format("`%s` != %s", column, value));
    }

    public SQLFilter or(SQLFilter... filters) {
        return raw("(" + Joiner.on(") OR (").join(filters) + ")");
    }

    public SQLFilter or(List<SQLFilter> filters) {
        return raw("(" + Joiner.on(") OR (").join(filters) + ")");
    }

    public SQLFilter and(SQLFilter... filters) {
        return raw("(" + Joiner.on(") AND (").join(filters) + ")");
    }

    public SQLFilter and(List<SQLFilter> filters) {
        return raw("(" + Joiner.on(") AND (").join(filters) + ")");
    }

    public SQLFilter raw(String filter) {
        SQLFilter clone = new SQLFilter(this);
        clone.filter = filter;
        return clone;
    }

    @Override
    public String toString() {
        return filter;
    }
}