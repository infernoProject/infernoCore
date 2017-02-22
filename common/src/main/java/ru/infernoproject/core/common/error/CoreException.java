package ru.infernoproject.core.common.error;

import org.slf4j.Logger;

import javax.script.ScriptException;
import java.sql.SQLException;

public class CoreException extends Exception {

    private final Exception exception;

    public CoreException(Exception exception) {
        this.exception = exception;
    }

    public void log(Logger logger) {
        logger.error(this.toString());
    }

    @Override
    public String toString() {
        if (exception.getClass().equals(SQLException.class)) {
            SQLException exc = (SQLException) exception;
            return String.format("SQLError[%s]: %s", exc.getSQLState(), exc.getMessage());
        } else if (exception.getClass().equals(ScriptException.class)) {
            ScriptException exc = (ScriptException) exception;
            return String.format("ScriptException[%s]: %s", exc.getLineNumber(), exc.getMessage());
        } else {
            return String.format("Error[%s]: %s", exception.getClass().getSimpleName(), exception.getMessage());
        }
    }
}
