package ru.infernoproject.worldd.script;

import javax.script.ScriptException;

public class ScriptValidationResult {

    private final boolean valid;

    private final int line;
    private final int column;
    private final String message;

    public ScriptValidationResult() {
        valid = true;

        line = 0;
        column = 0;
        message = null;
    }

    public ScriptValidationResult(ScriptException exc) {
        valid = false;

        line = exc.getLineNumber();
        column = exc.getColumnNumber();
        message = exc.getMessage().replace(String.format("<eval>:%d:%d ", line, column), "");
    }

    public ScriptValidationResult(String reason) {
        valid = true;

        line = 0;
        column = 0;
        message = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }
}
