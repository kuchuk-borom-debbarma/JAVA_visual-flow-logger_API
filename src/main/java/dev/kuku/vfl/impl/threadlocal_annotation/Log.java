package dev.kuku.vfl.impl.threadlocal_annotation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static class for logging
 */
public class Log {
    public static void Info(String message) {
        ContextManager.logger.info(message);
    }

    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.infoFn(fn, messageSerializer);
    }

    public static void Warn(String message) {
        ContextManager.logger.warn(message);
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.warnFn(fn, messageSerializer);
    }

    public static void Error(String message) {
        ContextManager.logger.error(message);
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.errorFn(fn, messageSerializer);
    }
}