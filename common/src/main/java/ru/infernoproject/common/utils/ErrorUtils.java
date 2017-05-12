package ru.infernoproject.common.utils;

import com.google.common.base.Joiner;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class ErrorUtils {

    private final Logger logger;

    private ErrorUtils(Logger logger) {
        this.logger = logger;
    }

    public static ErrorUtils logger(Logger logger) {
        return new ErrorUtils(logger);
    }

    public void error(String message, Throwable cause) {
        if (cause.getClass().equals(InvocationTargetException.class)) {
            log(message, ((InvocationTargetException) cause).getTargetException());
        } else {
            log(message, cause);
        }
    }

    private void log(String message, Throwable cause) {
        logger.error(String.format(
            "%s: [%s]: %s", message,
            cause.getClass().getSimpleName(), cause.getMessage())
        );

        logger.error(generateTrace(cause));
    }

    private String generateTrace(Throwable throwable) {
        List<String> sTraceStrings = new ArrayList<>();

        for (StackTraceElement sTrace: throwable.getStackTrace()) {
            sTraceStrings.add(String.format(
                    "%s:%d - %s - %s",
                    sTrace.getFileName(), sTrace.getLineNumber(),
                    sTrace.getClassName(), sTrace.getMethodName()
            ));
        }

        return "StackTrace:\n\n\t" + Joiner.on("\n\t").join(sTraceStrings) + "\n\n";
    }
}
