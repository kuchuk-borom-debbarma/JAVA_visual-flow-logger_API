package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static wrapper around ThreadVFL that provides convenient static access to all logging operations.
 * <p>
 * This class delegates all operations to the current thread's ThreadVFL logger instance,
 * providing a clean static API while maintaining all the thread-local context management
 * and hierarchical logging capabilities of the underlying ThreadVFL implementation.
 *
 * <p>All method names use uppercase starting letters to distinguish them from instance methods
 * and follow static method naming conventions.
 *
 * <p><strong>Prerequisites:</strong> The current thread must have been initialized with
 * {@link ThreadVFL#initializeForCurrentThread(VFLBlockContext)} before using any of these methods.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Initialize the thread logger first
 * ThreadVFL.initializeForCurrentThread(rootContext);
 *
 * // Then use static methods
 * StaticThreadVFL.Log("Starting operation");
 * String result = StaticThreadVFL.Supply("ProcessData", () -> processData());
 * StaticThreadVFL.Run("CleanupTask", () -> cleanup());
 * }</pre>
 *
 * @author Kuchuk Boram Debbarma
 * @see ThreadVFL
 * @since 1.0
 */
public final class StaticThreadVFL {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private StaticThreadVFL() {
        throw new UnsupportedOperationException("StaticThreadVFL is a utility class and cannot be instantiated");
    }

    /**
     * Gets the current ThreadVFL logger instance for delegation.
     *
     * @return the current thread's ThreadVFL logger
     * @throws IllegalStateException if no logger is initialized for the current thread
     */
    private static ThreadVFL getCurrentLogger() {
        return ThreadVFL.getCurrentLogger();
    }

    // ========== INITIALIZATION AND CLEANUP METHODS ==========

    /**
     * Initializes the logger stack for the current thread with a root logger instance.
     *
     * @param rootContext the root execution context to initialize the logger stack with
     * @throws IllegalArgumentException if rootContext is null
     * @throws IllegalStateException if a logger stack is already initialized for the current thread
     * @see ThreadVFL#initializeForCurrentThread(VFLBlockContext)
     */
    public static void InitializeForCurrentThread(VFLBlockContext rootContext) {
        ThreadVFL.initializeForCurrentThread(rootContext);
    }

    /**
     * Clears the logger stack for the current thread, releasing ThreadLocal resources.
     *
     * @see ThreadVFL#clearCurrentThread()
     */
    public static void ClearCurrentThread() {
        ThreadVFL.clearCurrentThread();
    }

    /**
     * Returns the current depth of the logger stack for diagnostic purposes.
     *
     * @return the number of nested logger contexts in the current thread's stack
     * @throws IllegalStateException if no logger stack is initialized for the current thread
     * @see ThreadVFL#getStackDepth()
     */
    public static int GetStackDepth() {
        return ThreadVFL.getStackDepth();
    }

    // ========== BASIC LOGGING METHODS ==========

    /**
     * Logs a message at the standard MESSAGE level.
     *
     * @param message the message to log
     */
    public static void Log(String message) {
        getCurrentLogger().log(message);
    }

    /**
     * Executes a function and logs its result at the MESSAGE level.
     *
     * @param <R> the return type of the function
     * @param fn the function to execute
     * @param messageSerializer function to convert the result to a log message
     * @return the result of the executed function
     */
    public static <R> R LogFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return getCurrentLogger().logFn(fn, messageSerializer);
    }

    /**
     * Logs a warning message at the WARN level.
     *
     * @param message the warning message to log
     */
    public static void Warn(String message) {
        getCurrentLogger().warn(message);
    }

    /**
     * Executes a function and logs its result at the WARN level.
     *
     * @param <R> the return type of the function
     * @param fn the function to execute
     * @param messageSerializer function to convert the result to a warning message
     * @return the result of the executed function
     */
    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return getCurrentLogger().warnFn(fn, messageSerializer);
    }

    /**
     * Logs an error message at the ERROR level.
     *
     * @param message the error message to log
     */
    public static void Error(String message) {
        getCurrentLogger().error(message);
    }

    /**
     * Executes a function and logs its result at the ERROR level.
     *
     * @param <R> the return type of the function
     * @param fn the function to execute
     * @param messageSerializer function to convert the result to an error message
     * @return the result of the executed function
     */
    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return getCurrentLogger().errorFn(fn, messageSerializer);
    }

    /**
     * Closes the current logging block with an optional end message.
     *
     * @param endMessage optional message to log when closing the block
     */
    public static void Close(String endMessage) {
        getCurrentLogger().close(endMessage);
    }

    // ========== SUPPLY METHOD OVERLOADS ==========

    /**
     * Executes a supplier within a sub-block with full parameter control.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage optional message logged when sub-block starts
     * @param supplier the operation to execute within the sub-block context
     * @param blockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer optional function to format the result for logging
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier,
                               LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supply(subBlockName, subBlockStartMessage, supplier, blockStartType, endMessageSerializer);
    }

    /**
     * Executes a supplier within a sub-block - minimal overload.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, Supplier<R> supplier) {
        return getCurrentLogger().supply(subBlockName, supplier);
    }

    /**
     * Executes a supplier within a sub-block with start message.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        return getCurrentLogger().supply(subBlockName, subBlockStartMessage, supplier);
    }

    /**
     * Executes a supplier within a sub-block with block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute
     * @param blockStartType determines logging behavior and execution flow control
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return getCurrentLogger().supply(subBlockName, supplier, blockStartType);
    }

    /**
     * Executes a supplier within a sub-block with end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute
     * @param endMessageSerializer function to format the result for logging
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supply(subBlockName, supplier, endMessageSerializer);
    }

    /**
     * Executes a supplier within a sub-block with start message and block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute
     * @param blockStartType determines logging behavior and execution flow control
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier,
                               LogTypeBlockStartEnum blockStartType) {
        return getCurrentLogger().supply(subBlockName, subBlockStartMessage, supplier, blockStartType);
    }

    /**
     * Executes a supplier within a sub-block with start message and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute
     * @param endMessageSerializer function to format the result for logging
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier,
                               Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supply(subBlockName, subBlockStartMessage, supplier, endMessageSerializer);
    }

    /**
     * Executes a supplier within a sub-block with block start type and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute
     * @param blockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer function to format the result for logging
     * @return the result returned by the supplier
     */
    public static <R> R Supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType,
                               Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supply(subBlockName, supplier, blockStartType, endMessageSerializer);
    }

    // ========== RUN METHOD OVERLOADS ==========

    /**
     * Executes a runnable within a sub-block with full parameter control.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute
     * @param blockStartType determines logging behavior and execution flow control
     */
    public static void Run(String subBlockName, String subBlockStartMessage, Runnable runnable,
                           LogTypeBlockStartEnum blockStartType) {
        getCurrentLogger().run(subBlockName, subBlockStartMessage, runnable, blockStartType);
    }

    /**
     * Executes a runnable within a sub-block - minimal overload.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute
     */
    public static void Run(String subBlockName, Runnable runnable) {
        getCurrentLogger().run(subBlockName, runnable);
    }

    /**
     * Executes a runnable within a sub-block with start message.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute
     */
    public static void Run(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        getCurrentLogger().run(subBlockName, subBlockStartMessage, runnable);
    }

    /**
     * Executes a runnable within a sub-block with block start type.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute
     * @param blockStartType determines logging behavior and execution flow control
     */
    public static void Run(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        getCurrentLogger().run(subBlockName, runnable, blockStartType);
    }

    // ========== ASYNC SUPPLY METHOD OVERLOADS ==========

    /**
     * Executes a supplier asynchronously within a sub-block with full parameter control.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param subBlockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer function to format the result for logging
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage,
                                                       Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType,
                                                       Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supplyAsync(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer);
    }

    /**
     * Executes a supplier asynchronously within a sub-block - minimal overload.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier) {
        return getCurrentLogger().supplyAsync(subBlockName, supplier);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with start message.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        return getCurrentLogger().supplyAsync(subBlockName, subBlockStartMessage, supplier);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return getCurrentLogger().supplyAsync(subBlockName, supplier, blockStartType);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param endMessageSerializer function to format the result for logging
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supplyAsync(subBlockName, supplier, endMessageSerializer);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with start message and block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage,
                                                       Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return getCurrentLogger().supplyAsync(subBlockName, subBlockStartMessage, supplier, blockStartType);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with start message and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param endMessageSerializer function to format the result for logging
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage,
                                                       Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supplyAsync(subBlockName, subBlockStartMessage, supplier, endMessageSerializer);
    }

    /**
     * Executes a supplier asynchronously within a sub-block with block start type and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer function to format the result for logging
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier,
                                                       LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return getCurrentLogger().supplyAsync(subBlockName, supplier, blockStartType, endMessageSerializer);
    }

    // ========== ASYNC SUPPLY WITH EXECUTOR METHOD OVERLOADS ==========

    /**
     * Executes a supplier asynchronously with a custom executor - full parameter control.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param subBlockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer function to format the result for logging
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                           Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType,
                                                           Function<R, String> endMessageSerializer, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor - minimal overload.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, supplier, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor and start message.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                           Supplier<R> supplier, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor and block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                           LogTypeBlockStartEnum blockStartType, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, supplier, blockStartType, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param endMessageSerializer function to format the result for logging
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                           Function<R, String> endMessageSerializer, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, supplier, endMessageSerializer, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor, start message, and block start type.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                           Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, blockStartType, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor, start message, and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param supplier the operation to execute asynchronously
     * @param endMessageSerializer function to format the result for logging
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                           Supplier<R> supplier, Function<R, String> endMessageSerializer, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, endMessageSerializer, executor);
    }

    /**
     * Executes a supplier asynchronously with a custom executor, block start type, and end message serializer.
     *
     * @param <R> the return type of the supplier
     * @param subBlockName descriptive name for the sub-block
     * @param supplier the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @param endMessageSerializer function to format the result for logging
     * @param executor the executor to run the async operation on
     * @return CompletableFuture containing the result
     */
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                           LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer, Executor executor) {
        return getCurrentLogger().supplyAsyncWith(subBlockName, supplier, blockStartType, endMessageSerializer, executor);
    }

    // ========== ASYNC RUN METHOD OVERLOADS ==========

    /**
     * Executes a runnable asynchronously within a sub-block with full parameter control.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute asynchronously
     * @param subBlockStartType determines logging behavior and execution flow control
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsync(String subBlockName, String subBlockStartMessage,
                                                   Runnable runnable, LogTypeBlockStartEnum subBlockStartType) {
        return getCurrentLogger().runAsync(subBlockName, subBlockStartMessage, runnable, subBlockStartType);
    }

    /**
     * Executes a runnable asynchronously within a sub-block - minimal overload.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute asynchronously
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsync(String subBlockName, Runnable runnable) {
        return getCurrentLogger().runAsync(subBlockName, runnable);
    }

    /**
     * Executes a runnable asynchronously within a sub-block with start message.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute asynchronously
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsync(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        return getCurrentLogger().runAsync(subBlockName, subBlockStartMessage, runnable);
    }

    /**
     * Executes a runnable asynchronously within a sub-block with block start type.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsync(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        return getCurrentLogger().runAsync(subBlockName, runnable, blockStartType);
    }

    // ========== ASYNC RUN WITH EXECUTOR METHOD OVERLOADS ==========

    /**
     * Executes a runnable asynchronously with a custom executor - full parameter control.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute asynchronously
     * @param subBlockStartType determines logging behavior and execution flow control
     * @param executor the executor to run the async operation on
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, String subBlockStartMessage,
                                                       Runnable runnable, LogTypeBlockStartEnum subBlockStartType,
                                                       Executor executor) {
        return getCurrentLogger().runAsyncWith(subBlockName, subBlockStartMessage, runnable, subBlockStartType, executor);
    }

    /**
     * Executes a runnable asynchronously with a custom executor - minimal overload.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute asynchronously
     * @param executor the executor to run the async operation on
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, Runnable runnable, Executor executor) {
        return getCurrentLogger().runAsyncWith(subBlockName, runnable, executor);
    }

    /**
     * Executes a runnable asynchronously with a custom executor and start message.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param subBlockStartMessage message logged when sub-block starts
     * @param runnable the operation to execute asynchronously
     * @param executor the executor to run the async operation on
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, String subBlockStartMessage,
                                                       Runnable runnable, Executor executor) {
        return getCurrentLogger().runAsyncWith(subBlockName, subBlockStartMessage, runnable, executor);
    }

    /**
     * Executes a runnable asynchronously with a custom executor and block start type.
     *
     * @param subBlockName descriptive name for the sub-block
     * @param runnable the operation to execute asynchronously
     * @param blockStartType determines logging behavior and execution flow control
     * @param executor the executor to run the async operation on
     * @return CompletableFuture for the async operation
     */
    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, Runnable runnable,
                                                       LogTypeBlockStartEnum blockStartType, Executor executor) {
        return getCurrentLogger().runAsyncWith(subBlockName, runnable, blockStartType, executor);
    }

    // ========== EVENT PUBLISHER METHODS ==========

    /**
     * Creates and registers an event publisher block for asynchronous event-driven execution.
     *
     * <p>The returned {@link EventPublisherBlock} must be used with the appropriate runner methods
     * to ensure proper logger initialization and context propagation.
     *
     * @param eventBranchName descriptive identifier for the event publishing branch
     * @param publishStartMessage message logged when the event publisher is created, or null for no message
     * @return {@link EventPublisherBlock} containing the context required by event listeners
     * @see EventPublisherBlock
     */
    public static EventPublisherBlock CreateEventPublisherBlock(String eventBranchName, String publishStartMessage) {
        return getCurrentLogger().createEventPublisherBlock(eventBranchName, publishStartMessage);
    }

    /**
     * Creates and registers an event publisher block for asynchronous event-driven execution - minimal overload.
     *
     * @param eventBranchName descriptive identifier for the event publishing branch
     * @return {@link EventPublisherBlock} containing the context required by event listeners
     * @see EventPublisherBlock
     */
    public static EventPublisherBlock CreateEventPublisherBlock(String eventBranchName) {
        return getCurrentLogger().createEventPublisherBlock(eventBranchName, null);
    }
}