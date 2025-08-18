package dev.kuku.vfl.core.helpers;

import dev.kuku.vfl.impl.annotation.SubBlock;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for Visual Flow Logger (VFL) operations.
 *
 * <p>This class centralizes:
 * <ul>
 *   <li>String/message formatting</li>
 *   <li>Argument and return-value placeholder resolution</li>
 *   <li>Thread info helpers</li>
 *   <li>ID trimming</li>
 * </ul>
 */
public class VFLHelper {

    // Regex for {0}, {1}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)}");
    // Regex for {r} or {return} placeholders
    private static final Pattern RETURN_PLACEHOLDER_PATTERN =
            Pattern.compile("\\{r(?:eturn)?}", Pattern.CASE_INSENSITIVE);

    // UUID v7 generator (time-based with Unix timestamp)
    private static final TimeBasedEpochGenerator UUID_V7_GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * Generates a UUID v7 (time-based with Unix timestamp) for better sorting and indexing.
     *
     * @return UUID v7 as string
     */
    public static String UID() {
        return UUID_V7_GENERATOR.generate().toString();
    }

    /**
     * Formats a SLF4J-style message string with "{}" placeholders.
     */
    public static String FormatMessage(String message, Object... args) {
        if (args == null || args.length == 0 || message == null) {
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
            String messageTemplate = msgSerializer.apply(r);
            Object[] allArgs = VFLHelper.CombineArgsWithReturn(args, r);
            return VFLHelper.FormatMessage(messageTemplate, allArgs);
        };
    }

    /**
     * Replace {0}, {1}, etc. placeholders with argument values.
     */
    public static String ReplaceArgPlaceholders(String text, Object[] args) {
        if (text == null || args == null || args.length == 0) {
            return text;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index;
            try {
                index = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            String replacement;
            if (index >= 0 && index < args.length) {
                Object arg = args[index];
                replacement = (arg == null) ? "null" : arg.toString();
            } else {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replace {r} or {return} placeholders with the string value of the return object.
     */
    public static String ReplaceReturnPlaceholder(String text, Object returnedValue) {
        if (text == null) {
            return null;
        }
        String replacement = (returnedValue == null) ? "null" : returnedValue.toString();
        return RETURN_PLACEHOLDER_PATTERN.matcher(text)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }

    /**
     * Resolves block name from annotation or method signature.
     */
    public static String ResolveBlockName(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        String rawName = (annotation != null && isValid(annotation.blockName()))
                ? annotation.blockName().trim()
                : GetMethodName(method, args);
        return ReplaceArgPlaceholders(rawName, args);
    }

    /**
     * Resolves start message from annotation.
     */
    public static String ResolveStartMessage(Method method, Object[] args) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        if (annotation != null && isValid(annotation.startMessage())) {
            String msg = annotation.startMessage().trim();
            return ReplaceArgPlaceholders(msg, args);
        }
        return null;
    }

    /**
     * Resolves end message from annotation with added return value.
     */
    public static String ResolveEndMessage(Method method, Object[] args, Object returnedValue) {
        SubBlock annotation = method.getAnnotation(SubBlock.class);
        if (annotation != null && isValid(annotation.endMessage())) {
            String msg = annotation.endMessage().trim();
            msg = ReplaceArgPlaceholders(msg, args);
            msg = ReplaceReturnPlaceholder(msg, returnedValue);
            return msg;
        }
        return null;
    }

    private static boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static String GetMethodName(Method method, Object[] args) {
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