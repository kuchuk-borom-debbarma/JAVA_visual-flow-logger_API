package dev.kuku.vfl.core.helpers;

import java.util.UUID;

public class Util {
    public static String UID() {
        return UUID.randomUUID().toString();
    }

    public static String FormatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        String result = message;
        for (Object arg : args) {
            if (result.contains("{}")) {
                result = result.replaceFirst("\\{\\}",
                        arg == null ? "null" : String.valueOf(arg));
            } else {
                break; // No more placeholders
            }
        }
        return result;
    }
}