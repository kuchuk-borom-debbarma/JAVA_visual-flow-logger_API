package dev.kuku.vfl.core.helpers;

import dev.kuku.vfl.impl.threadlocal.annotations.VFLBlock;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Function;

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
                result = result.replaceFirst("\\{}",
                        arg == null ? "null" : String.valueOf(arg));
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Combines user arguments with a return value into a single array.
     * The return value is appended as the last element.
     *
     * @param userArgs    The original arguments passed by the user
     * @param returnValue The return value to append
     * @return A new array containing all user args followed by the return value
     */
    public static Object[] CombineArgsWithReturn(Object[] userArgs, Object returnValue) {
        if (userArgs == null) {
            return new Object[]{returnValue};
        }

        Object[] combined = new Object[userArgs.length + 1];
        System.arraycopy(userArgs, 0, combined, 0, userArgs.length);
        combined[userArgs.length] = returnValue;
        return combined;
    }

    public static String GetThreadInfo() {
        Thread currentThread = Thread.currentThread();
        return String.format("[Thread: %s (ID: %d)]", currentThread.getName(), currentThread.threadId());
    }

    public static String TrimId(String fullId) {
        if (fullId == null) return "null";
        String[] parts = fullId.split("-");
        return parts.length > 0 ? parts[parts.length - 1] : fullId;
    }

    public static <R> Function<R, String> UpdateEndMsg(Function<R, String> msgSerializer, Object... args) {
        return (r) -> {
            // Get the message template from the user's serializer
            String messageTemplate = msgSerializer.apply(r);

            // Format the message with user args + return value
            // Args convention: user args fill {0}, {1}, {2}... and return value fills the last placeholder
            Object[] allArgs = Util.CombineArgsWithReturn(args, r);
            return Util.FormatMessage(messageTemplate, allArgs);
        };
    }


    public static String GetMethodName(Method method, Object[] args) {
        VFLBlock anno = method.getAnnotation(VFLBlock.class);
        if (anno != null && !anno.blockName().isEmpty()) {
            String name = anno.blockName();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    name = name.replace("{" + i + "}", String.valueOf(args[i]));
                }
            }
            return name;
        }

        StringBuilder sb = new StringBuilder(method.getName()).append('(');
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i]);
            }
        }
        return sb.append(')').toString();
    }

}