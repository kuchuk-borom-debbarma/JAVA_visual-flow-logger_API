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

    public static String getThreadInfo() {
        Thread currentThread = Thread.currentThread();
        return String.format("[Thread: %s (ID: %d)]", currentThread.getName(), currentThread.threadId());
    }

    public static String trimId(String fullId) {
        if (fullId == null) return "null";
        String[] parts = fullId.split("-");
        return parts.length > 0 ? parts[parts.length - 1] : fullId;
    }

}