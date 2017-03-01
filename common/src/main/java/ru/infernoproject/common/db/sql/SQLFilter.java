package ru.infernoproject.common.db.sql;

import com.google.common.base.Joiner;

import java.util.List;

public final class SQLFilter implements Cloneable {

    private String column;
    private String filter;

    public SQLFilter(String column) {
        this.column = column;
    }

    public SQLFilter() {}

    protected final SQLFilter clone() {
        try {
            return (SQLFilter) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    public SQLFilter like(String pattern) {
        return or(
            clone().raw(String.format("`%s` LIKE '%%%s%%'", column, pattern)),
            clone().raw(String.format("`%s` LIKE '%s%%'", column, pattern)),
            clone().raw(String.format("`%s` LIKE '%%%s'", column, pattern)),
            clone().raw(String.format("`%s` LIKE '%s'", column, pattern))
        );
    }

    public SQLFilter eq(String value) {
        return raw(String.format("`%s` = '%s'", column, value));
    }

    public SQLFilter eq(Integer value) {
        return raw(String.format("`%s` = %s", column, value));
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
        SQLFilter clone = clone();
        clone.filter = filter;
        return clone;
    }

    @Override
    public String toString() {
        return filter;
    }
}