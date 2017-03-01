package ru.infernoproject.common.utils;

import java.util.HashMap;
import java.util.Map;

public class Result {

    public enum State {
        SUCCESS, FAILED
    }

    private State status;
    private Map<String, Object> attributes;

    public Result(State status) {
        this.status = status;
        this.attributes = new HashMap<>();
    }

    public static Result success() {
        return new Result(State.SUCCESS);
    }

    public static Result failed() {
        return new Result(State.FAILED);
    }

    public Result attr(String name, Object value) {
        attributes.put(name, value);

        return this;
    }

    public State getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status.equals(State.SUCCESS);
    }

    public boolean isFailed() {
        return status.equals(State.FAILED);
    }

    public <T> T attr(Class<T> type, String name) {
        Object value = attributes.get(name);

        if (value == null)
            return null;

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(String.format(
                "Attribute '%s' expected to be '%s', but '%s' found",
                name, type.getSimpleName(), value.getClass().getSimpleName()
            ));
        }

        return type.cast(value);
    }

    public String attr(String name) {
        return attr(String.class, name);
    }

    public String message() {
        return attr("message");
    }

    public Result message(String message) {
        return attr("message", message);
    }
}
