package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;

import java.util.function.Function;
import java.util.function.Supplier;

public class Log {
    static VFL INSTANCE = new VFL() {
        @Override
        protected BlockContext getContext() {
            return ThreadContextManager.GetCurrentBlockContext();
        }

        @Override
        protected VFLBuffer getBuffer() {
            return Configuration.INSTANCE.buffer;
        }
    };

    // ================ INFO METHODS ================
    public static void Info(String message, Object... args) {
        if (!VFLInitializer.initialized) return;
        INSTANCE.info(Util.FormatMessage(message, args));
    }

    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();
        return INSTANCE.infoFn(fn, messageSerializer);
    }

    public static <R> R InfoFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return INSTANCE.infoFn(fn, s);
    }

    public static void InfoFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }

        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.infoFn(supplier, s);
    }

    // ================ WARN METHODS ================
    public static void Warn(String message, Object... args) {
        if (!VFLInitializer.initialized) return;

        INSTANCE.warn(Util.FormatMessage(message, args));
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();

        return INSTANCE.warnFn(fn, messageSerializer);
    }

    public static <R> R WarnFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();

        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return INSTANCE.warnFn(fn, s);
    }

    public static void WarnFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }

        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.warnFn(supplier, s);
    }

    // ================ ERROR METHODS ================
    public static void Error(String message, Object... args) {
        if (!VFLInitializer.initialized) return;

        INSTANCE.error(Util.FormatMessage(message, args));
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();

        return INSTANCE.errorFn(fn, messageSerializer);
    }

    public static <R> R ErrorFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();

        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return INSTANCE.errorFn(fn, s);
    }

    public static void ErrorFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }

        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.errorFn(supplier, s);
    }
}
