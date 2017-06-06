package ru.infernoproject.common.auth.sql;

public enum AccountLevel {
    USER, MODERATOR, GAME_MASTER, ADMIN;

    public static boolean isAdmin(AccountLevel level) {
        return toInt(level) >= toInt(ADMIN);
    }

    public static boolean isGameMaster(AccountLevel level) {
        return toInt(level) >= toInt(GAME_MASTER);
    }

    public static boolean isModerator(AccountLevel level) {
        return toInt(level) >= toInt(MODERATOR);
    }

    public static boolean hasAccess(AccountLevel level, AccountLevel minLevel) {
        return toInt(level) >= toInt(minLevel);
    }

    public static boolean hasAccess(AccountLevel level, int minLevel) {
        return toInt(level) >= minLevel;
    }

    public static int toInt(AccountLevel level) {
        switch (level) {
            case ADMIN:
                return 4;
            case GAME_MASTER:
                return 3;
            case MODERATOR:
                return 2;
            case USER:
                return 1;
            default:
                return 0;
        }
    }
}
