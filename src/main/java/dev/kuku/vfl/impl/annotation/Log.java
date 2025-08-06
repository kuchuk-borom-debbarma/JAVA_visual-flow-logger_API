package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.helpers.Util;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static class for logging
 */
public class Log {

    // ================ INFO METHODS ================
    public static void Info(String message, Object... args) {
        ContextManager.logger.info(Util.FormatMessage(message, args));
    }

    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.infoFn(fn, messageSerializer);
    }

    public static <R> R InfoFn(Supplier<R> fn, String message, Object... args) {
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.infoFn(fn, s);
    }

    // ================ WARN METHODS ================

    public static void Warn(String message, Object... args) {
        ContextManager.logger.warn(Util.FormatMessage(message, args));
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.warnFn(fn, messageSerializer);
    }

    public static <R> R WarnFn(Supplier<R> fn, String message, Object... args) {
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.warnFn(fn, s);
    }

    // ================ ERROR METHODS ================

    public static void Error(String message, Object... args) {
        ContextManager.logger.error(Util.FormatMessage(message, args));
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return ContextManager.logger.errorFn(fn, messageSerializer);
    }

    public static <R> R ErrorFn(Supplier<R> fn, String message, Object... args) {
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.errorFn(fn, s);
    }
}