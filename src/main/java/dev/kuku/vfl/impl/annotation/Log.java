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

    public static void InfoFn(Runnable runnable, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        ContextManager.logger.infoFn(supplier, s);
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

    public static void WarnFn(Runnable runnable, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        ContextManager.logger.warnFn(supplier, s);
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

    public static void ErrorFn(Runnable runnable, String message, Object... args) {
        if (!VFLAnnotationProcessor.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        ContextManager.logger.errorFn(supplier, s);
    }
}
