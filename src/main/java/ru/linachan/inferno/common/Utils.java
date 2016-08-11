package ru.linachan.inferno.common;

public class Utils {

    public static void assertNotNull(Object target, String message) {
        if (target == null) {
            throw new IllegalStateException(message);
        }
    }

    public static void assertNotNull(Object target) {
        if (target == null) {
            throw new IllegalStateException("It should not be null!");
        }
    }
}
