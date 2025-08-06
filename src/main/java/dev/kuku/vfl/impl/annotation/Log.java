package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.helpers.Util;

import java.util.function.Function;
import java.util.function.Supplier;

public class Log {
    // ================ INFO METHODS ================
    public static void Info(String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return;
        ContextManager.logger.info(Util.FormatMessage(message, args));
    }

    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        return ContextManager.logger.infoFn(fn, messageSerializer);
    }

    public static <R> R InfoFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.infoFn(fn, s);
    }

    // ================ WARN METHODS ================
    public static void Warn(String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return;
        ContextManager.logger.warn(Util.FormatMessage(message, args));
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        return ContextManager.logger.warnFn(fn, messageSerializer);
    }

    public static <R> R WarnFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.warnFn(fn, s);
    }

    // ================ ERROR METHODS ================
    public static void Error(String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return;
        ContextManager.logger.error(Util.FormatMessage(message, args));
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        return ContextManager.logger.errorFn(fn, messageSerializer);
    }

    public static <R> R ErrorFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) return fn.get();
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return ContextManager.logger.errorFn(fn, s);
    }
}
