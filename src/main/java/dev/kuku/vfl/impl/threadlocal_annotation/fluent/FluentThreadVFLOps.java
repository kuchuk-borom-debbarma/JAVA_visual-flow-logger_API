package dev.kuku.vfl.impl.threadlocal_annotation.fluent;

import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.ThreadVFLRunnableAsyncStep;
import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.ThreadVFLRunnableStep;
import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.ThreadVFLSupplierAsyncStep;
import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.ThreadVFLSupplierStep;
import dev.kuku.vfl.impl.threadlocal_annotation.logger.ThreadVFL;

import java.util.function.Supplier;

/**
 * Static facade for FluentThreadVFL providing convenient static access to logging operations.
 *
 * <p>This class provides static methods that map directly to the instance methods of
 * {@link FluentThreadVFL}. Each method creates a new FluentThreadVFL instance using
 * the current thread's logger context, making it convenient to use without explicitly
 * managing FluentThreadVFL instances.
 *
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * // Initialize thread context first
 * ThreadVFL.initializeForCurrentThread(rootContext);
 *
 * // Use static methods directly
 * String result = FluentThreadVFLOps.Call(() -> someOperation())
 *     .asSubBlock("ProcessData")
 *     .withStartMessage("Starting data processing")
 *     .withEndMessage("Completed processing: {0}")
 *     .execute();
 *
 * FluentThreadVFLOps.Run(() -> cleanupOperation())
 *     .asSubBlock("Cleanup")
 *     .execute();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as it delegates to
 * ThreadVFL which uses ThreadLocal storage for maintaining logger context.
 *
 * @author Kuchuk Boram Debbarma
 * @see FluentThreadVFL
 * @see ThreadVFL
 * @since 1.0
 */
public final class FluentThreadVFLOps {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FluentThreadVFLOps() {
        throw new UnsupportedOperationException("FluentThreadVFLOps is a utility class and cannot be instantiated");
    }

    /**
     * Logs a message with optional formatting arguments.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and delegates the logging operation to it.
     *
     * @param message the message template to log
     * @param args    optional arguments for message formatting using {0}, {1}, etc. placeholders
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * FluentThreadVFLOps.Log("Processing file {0} with {1} records", fileName, recordCount);
     * }</pre>
     */
    public static void Log(String message, Object... args) {
        new FluentThreadVFL().log(message, args);
    }

    /**
     * Logs a warning message with optional formatting arguments.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and delegates the warning operation to it.
     *
     * @param message the warning message template to log
     * @param args    optional arguments for message formatting using {0}, {1}, etc. placeholders
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * FluentThreadVFLOps.Warn("Cache miss for key {0}, using fallback", cacheKey);
     * }</pre>
     */
    public static void Warn(String message, Object... args) {
        new FluentThreadVFL().warn(message, args);
    }

    /**
     * Logs an error message with optional formatting arguments.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and delegates the error logging operation to it.
     *
     * @param message the error message template to log
     * @param args    optional arguments for message formatting using {0}, {1}, etc. placeholders
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * FluentThreadVFLOps.Error("Failed to process file {0}: {1}", fileName, errorMessage);
     * }</pre>
     */
    public static void Error(String message, Object... args) {
        new FluentThreadVFL().error(message, args);
    }

    /**
     * Creates a supplier step for synchronous execution with logging capabilities.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and returns a ThreadVFLSupplierStep that can be configured for sub-block execution
     * or direct logging.
     *
     * @param <R> the return type of the supplier function
     * @param fn  the supplier function to be executed
     * @return a ThreadVFLSupplierStep for further configuration and execution
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * String result = FluentThreadVFLOps.Call(() -> processData())
     *     .asSubBlock("DataProcessing")
     *     .withStartMessage("Starting data processing")
     *     .withEndMessage("Processing completed: {0}")
     *     .execute();
     * }</pre>
     */
    public static <R> ThreadVFLSupplierStep<R> Call(Supplier<R> fn) {
        return new FluentThreadVFL().call(fn);
    }

    /**
     * Creates a supplier step for asynchronous execution with logging capabilities.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and returns a ThreadVFLSupplierAsyncStep that can be configured for async
     * sub-block execution with CompletableFuture support.
     *
     * @param <R> the return type of the supplier function
     * @param fn  the supplier function to be executed asynchronously
     * @return a ThreadVFLSupplierAsyncStep for further configuration and async execution
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * CompletableFuture<String> futureResult = FluentThreadVFLOps.CallAsync(() -> processDataAsync())
     *     .asSubBlock("AsyncDataProcessing")
     *     .withStartMessage("Starting async data processing")
     *     .withEndMessage("Async processing completed: {0}")
     *     .executeDetached();
     * }</pre>
     */
    public static <R> ThreadVFLSupplierAsyncStep<R> CallAsync(Supplier<R> fn) {
        return new FluentThreadVFL().callAsync(fn);
    }

    /**
     * Creates a runnable step for synchronous execution with logging capabilities.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and returns a ThreadVFLRunnableStep that can be configured for sub-block execution
     * or direct logging.
     *
     * @param runnable the runnable operation to be executed
     * @return a ThreadVFLRunnableStep for further configuration and execution
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * FluentThreadVFLOps.Run(() -> cleanupResources())
     *     .asSubBlock("Cleanup")
     *     .withStartMessage("Starting cleanup process")
     *     .execute();
     * }</pre>
     */
    public static ThreadVFLRunnableStep Run(Runnable runnable) {
        return new FluentThreadVFL().run(runnable);
    }

    /**
     * Creates a runnable step for asynchronous execution with logging capabilities.
     *
     * <p>Creates a new FluentThreadVFL instance using the current thread's logger
     * and returns a ThreadVFLRunnableAsyncStep that can be configured for async
     * sub-block execution with CompletableFuture support.
     *
     * @param runnable the runnable operation to be executed asynchronously
     * @return a ThreadVFLRunnableAsyncStep for further configuration and async execution
     * @throws IllegalStateException if no logger context is initialized for the current thread
     * @example <pre>{@code
     * CompletableFuture<Void> futureResult = FluentThreadVFLOps.RunAsync(() -> cleanupResourcesAsync())
     *     .asSubBlock("AsyncCleanup")
     *     .withStartMessage("Starting async cleanup process")
     *     .withExecutor(customExecutor)
     *     .executeDetached();
     * }</pre>
     */
    public static ThreadVFLRunnableAsyncStep RunAsync(Runnable runnable) {
        return new FluentThreadVFL().runAsync(runnable);
    }
}