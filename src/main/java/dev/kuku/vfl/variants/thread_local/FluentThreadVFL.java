package dev.kuku.vfl.variants.thread_local;


import dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable;
import dev.kuku.vfl.core.fluent_api.callable.steps.CallableSupplierStep;
import dev.kuku.vfl.core.fluent_api.callable.steps.runnable.RunSubBlockStep;

import java.util.function.Supplier;

public class FluentThreadVFL {

    /**
     * Get the current fluent API instance from the thread local logger
     */
    private static FluentVFLCallable getCurrentFluentApi() {
        ThreadVFL currentLogger = ThreadVFL.getCurrentLogger();
        return new FluentVFLCallable(currentLogger);
    }

    // ==================== Static Logging Methods ====================

    /**
     * Log a message at INFO level
     */
    public static void Log(String message, Object... args) {
        getCurrentFluentApi().log(message, args);
    }

    /**
     * Log a message at WARN level
     */
    public static void Warn(String message, Object... args) {
        getCurrentFluentApi().warn(message, args);
    }

    /**
     * Log a message at ERROR level
     */
    public static void Error(String message) {
        getCurrentFluentApi().error(message);
    }

    // ==================== Static Fluent Call Methods ====================

    /**
     * Start a fluent call chain with a supplier
     */
    public static <R> CallableSupplierStep<R> Call(Supplier<R> fn) {
        return getCurrentFluentApi().call(fn);
    }

    /**
     * Start a fluent runnable sub block chain
     */
    public static RunSubBlockStep RunSubBlock(Runnable r) {
        return getCurrentFluentApi().runSubBlock(r);
    }
}
