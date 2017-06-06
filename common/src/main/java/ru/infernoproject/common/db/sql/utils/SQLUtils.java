package ru.infernoproject.common.db.sql.utils;

public class SQLUtils {

    public static String escapeString(String string) {
        if (isEscapeNeeded(string)) {
            StringBuilder buf = new StringBuilder((int)((double)string.length() * 1.1D));
            for (char chr: string.toCharArray()) {
                switch(chr) {
                    case '\u0000':
                        buf.append('\\');
                        buf.append('0');
                        break;
                    case '\n':
                        buf.append('\\');
                        buf.append('n');
                        break;
                    case '\r':
                        buf.append('\\');
                        buf.append('r');
                        break;
                    case '\u001a':
                        buf.append('\\');
                        buf.append('Z');
                        break;
                    case '\'':
                        buf.append('\\');
                        buf.append('\'');
                        break;
                    case '\\':
                        buf.append('\\');
                        buf.append('\\');
                        break;
                    default:
                        buf.append(chr);
                        break;
                }

            }

            string = buf.toString();
        }

        return string;
    }

    public static boolean isEscapeNeeded(String string) {
        boolean needsHexEscape = false;

        for (char chr: string.toCharArray()) {
            switch (chr) {
                case '\u0000':
                    needsHexEscape = true;
                    break;
                case '\n':
                    needsHexEscape = true;
                    break;
                case '\r':
                    needsHexEscape = true;
                    break;
                case '\u001a':
                    needsHexEscape = true;
                    break;
                case '"':
                    needsHexEscape = true;
                    break;
                case '\'':
                    needsHexEscape = true;
                    break;
                case '\\':
                    needsHexEscape = true;
                    break;
            }

            if (needsHexEscape) {
                break;
            }
        }

        return needsHexEscape;
    }
}
