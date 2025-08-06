package dev.kuku.vfl.impl.threadlocal.logger;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.TrimId;
import static dev.kuku.vfl.impl.threadlocal.logger.ThreadVFL.getCurrentLogger;

@Slf4j
public final class ThreadVFLOps {

    private ThreadVFLOps() {
        throw new UnsupportedOperationException("ThreadVFLOps is a utility class and cannot be instantiated");
    }

    private static ThreadVFL getLoggerWithValidation(String operation) {
        String threadInfo = GetThreadInfo();
        log.trace("Getting current logger for operation '{}' {}", operation, threadInfo);

        ThreadVFL logger = getCurrentLogger();
        if (logger == null) {
            log.error("CRITICAL: No current logger found for operation '{}' {} - This indicates VFL context is not properly initialized",
                    operation, threadInfo);
            throw new IllegalStateException("No VFL logger context available for operation: " + operation);
        }

        String loggerId = TrimId(logger.loggerContext.blockInfo.getId());
        log.trace("Retrieved logger '{}' for operation '{}' {}", loggerId, operation, threadInfo);
        return logger;
    }

    private static void logMethodEntry(String methodName, Object... params) {
        if (log.isDebugEnabled()) {
            StringBuilder paramStr = new StringBuilder();
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) paramStr.append(", ");
                paramStr.append(params[i]).append("=");
                if (i + 1 < params.length) {
                    Object value = params[i + 1];
                    if (value instanceof String) {
                        paramStr.append("'").append(value).append("'");
                    } else if (value != null) {
                        paramStr.append(value.getClass().getSimpleName());
                    } else {
                        paramStr.append("null");
                    }
                }
            }
            log.debug("ThreadVFLOps.{} called {} - Params: [{}]", methodName, GetThreadInfo(), paramStr);
        }
    }

    // ========== BASIC LOGGING METHODS ==========

    public static void Log(String message) {
        logMethodEntry("Log", "message", message);
        ThreadVFL logger = getLoggerWithValidation("Log");
        log.trace("Executing log operation with message length: {}", message != null ? message.length() : 0);
        logger.log(message);
        log.trace("Log operation completed successfully");
    }

    public static <R> R LogFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        logMethodEntry("LogFn", "fn", fn, "messageSerializer", messageSerializer);
        ThreadVFL logger = getLoggerWithValidation("LogFn");
        log.trace("Executing LogFn operation");
        try {
            R result = logger.logFn(fn, messageSerializer);
            log.trace("LogFn operation completed - Result type: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("LogFn operation failed: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static void Warn(String message) {
        logMethodEntry("Warn", "message", message);
        ThreadVFL logger = getLoggerWithValidation("Warn");
        log.trace("Executing warn operation with message length: {}", message != null ? message.length() : 0);
        logger.warn(message);
        log.trace("Warn operation completed successfully");
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        logMethodEntry("WarnFn", "fn", fn, "messageSerializer", messageSerializer);
        ThreadVFL logger = getLoggerWithValidation("WarnFn");
        log.trace("Executing WarnFn operation");
        try {
            R result = logger.warnFn(fn, messageSerializer);
            log.trace("WarnFn operation completed - Result type: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("WarnFn operation failed: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static void Error(String message) {
        logMethodEntry("Error", "message", message);
        ThreadVFL logger = getLoggerWithValidation("Error");
        log.trace("Executing error operation with message length: {}", message != null ? message.length() : 0);
        logger.error(message);
        log.trace("Error operation completed successfully");
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        logMethodEntry("ErrorFn", "fn", fn, "messageSerializer", messageSerializer);
        ThreadVFL logger = getLoggerWithValidation("ErrorFn");
        log.trace("Executing ErrorFn operation");
        try {
            R result = logger.errorFn(fn, messageSerializer);
            log.trace("ErrorFn operation completed - Result type: {}",
                    result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("ErrorFn operation failed: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // ========== SUPPLY METHOD OVERLOADS ==========

    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        logMethodEntry("Supply[Full]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "blockStartType", blockStartType, "supplier", supplier, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("Supply[Full]");
        log.debug("Executing full Supply operation for sub-block: '{}' - Start type: {}", subBlockName, blockStartType);
        try {
            R result = logger.supply(subBlockName, subBlockStartMessage, supplier, blockStartType, endMessageSerializer);
            log.debug("Supply[Full] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[Full] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, Supplier<R> supplier) {
        logMethodEntry("Supply[Basic]", "subBlockName", subBlockName, "supplier", supplier);
        ThreadVFL logger = getLoggerWithValidation("Supply[Basic]");
        log.debug("Executing basic Supply operation for sub-block: '{}'", subBlockName);
        try {
            R result = logger.supply(subBlockName, supplier);
            log.debug("Supply[Basic] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[Basic] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        logMethodEntry("Supply[WithMessage]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage, "supplier", supplier);
        ThreadVFL logger = getLoggerWithValidation("Supply[WithMessage]");
        log.debug("Executing Supply with message for sub-block: '{}' - Message: '{}'", subBlockName, subBlockStartMessage);
        try {
            R result = logger.supply(subBlockName, subBlockStartMessage, supplier);
            log.debug("Supply[WithMessage] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[WithMessage] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("Supply[WithType]", "subBlockName", subBlockName, "supplier", supplier, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("Supply[WithType]");
        log.debug("Executing Supply with type for sub-block: '{}' - Start type: {}", subBlockName, blockStartType);
        try {
            R result = logger.supply(subBlockName, supplier, blockStartType);
            log.debug("Supply[WithType] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[WithType] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        logMethodEntry("Supply[WithSerializer]", "subBlockName", subBlockName, "supplier", supplier, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("Supply[WithSerializer]");
        log.debug("Executing Supply with serializer for sub-block: '{}'", subBlockName);
        try {
            R result = logger.supply(subBlockName, supplier, endMessageSerializer);
            log.debug("Supply[WithSerializer] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[WithSerializer] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("Supply[MessageType]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "supplier", supplier, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("Supply[MessageType]");
        log.debug("Executing Supply with message and type for sub-block: '{}' - Message: '{}' - Type: {}",
                subBlockName, subBlockStartMessage, blockStartType);
        try {
            R result = logger.supply(subBlockName, subBlockStartMessage, supplier, blockStartType);
            log.debug("Supply[MessageType] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[MessageType] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        logMethodEntry("Supply[MessageSerializer]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "supplier", supplier, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("Supply[MessageSerializer]");
        log.debug("Executing Supply with message and serializer for sub-block: '{}' - Message: '{}'",
                subBlockName, subBlockStartMessage);
        try {
            R result = logger.supply(subBlockName, subBlockStartMessage, supplier, endMessageSerializer);
            log.debug("Supply[MessageSerializer] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[MessageSerializer] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> R Supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        logMethodEntry("Supply[TypeSerializer]", "subBlockName", subBlockName, "supplier", supplier,
                "blockStartType", blockStartType, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("Supply[TypeSerializer]");
        log.debug("Executing Supply with type and serializer for sub-block: '{}' - Type: {}", subBlockName, blockStartType);
        try {
            R result = logger.supply(subBlockName, supplier, blockStartType, endMessageSerializer);
            log.debug("Supply[TypeSerializer] operation completed for sub-block: '{}' - Result type: {}",
                    subBlockName, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Supply[TypeSerializer] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // ========== RUN METHOD OVERLOADS ==========

    public static void Run(String subBlockName, String subBlockStartMessage, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("Run[Full]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "runnable", runnable, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("Run[Full]");
        log.debug("Executing full Run operation for sub-block: '{}' - Message: '{}' - Type: {}",
                subBlockName, subBlockStartMessage, blockStartType);
        try {
            logger.run(subBlockName, subBlockStartMessage, runnable, blockStartType);
            log.debug("Run[Full] operation completed for sub-block: '{}'", subBlockName);
        } catch (Exception e) {
            log.error("Run[Full] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static void Run(String subBlockName, Runnable runnable) {
        logMethodEntry("Run[Basic]", "subBlockName", subBlockName, "runnable", runnable);
        ThreadVFL logger = getLoggerWithValidation("Run[Basic]");
        log.debug("Executing basic Run operation for sub-block: '{}'", subBlockName);
        try {
            logger.run(subBlockName, runnable);
            log.debug("Run[Basic] operation completed for sub-block: '{}'", subBlockName);
        } catch (Exception e) {
            log.error("Run[Basic] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static void Run(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        logMethodEntry("Run[WithMessage]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage, "runnable", runnable);
        ThreadVFL logger = getLoggerWithValidation("Run[WithMessage]");
        log.debug("Executing Run with message for sub-block: '{}' - Message: '{}'", subBlockName, subBlockStartMessage);
        try {
            logger.run(subBlockName, subBlockStartMessage, runnable);
            log.debug("Run[WithMessage] operation completed for sub-block: '{}'", subBlockName);
        } catch (Exception e) {
            log.error("Run[WithMessage] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static void Run(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("Run[WithType]", "subBlockName", subBlockName, "runnable", runnable, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("Run[WithType]");
        log.debug("Executing Run with type for sub-block: '{}' - Type: {}", subBlockName, blockStartType);
        try {
            logger.run(subBlockName, runnable, blockStartType);
            log.debug("Run[WithType] operation completed for sub-block: '{}'", subBlockName);
        } catch (Exception e) {
            log.error("Run[WithType] operation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // ========== ASYNC SUPPLY METHOD OVERLOADS ==========

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType, Function<R, String> endMessageSerializer) {
        logMethodEntry("SupplyAsync[Full]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "supplier", supplier, "subBlockStartType", subBlockStartType, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[Full]");
        log.debug("Starting SupplyAsync[Full] for sub-block: '{}' - Message: '{}' - Type: {}",
                subBlockName, subBlockStartMessage, subBlockStartType);
        try {
            CompletableFuture<R> future = logger.supplyAsync(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer);
            log.debug("SupplyAsync[Full] initiated for sub-block: '{}' - Future created", subBlockName);
            return future;
        } catch (Exception e) {
            log.error("SupplyAsync[Full] initiation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier) {
        logMethodEntry("SupplyAsync[Basic]", "subBlockName", subBlockName, "supplier", supplier);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[Basic]");
        log.debug("Starting SupplyAsync[Basic] for sub-block: '{}'", subBlockName);
        try {
            CompletableFuture<R> future = logger.supplyAsync(subBlockName, supplier);
            log.debug("SupplyAsync[Basic] initiated for sub-block: '{}' - Future created", subBlockName);
            return future;
        } catch (Exception e) {
            log.error("SupplyAsync[Basic] initiation failed for sub-block '{}': {} - Exception type: {}",
                    subBlockName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // Similar pattern for other async methods...
    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        logMethodEntry("SupplyAsync[WithMessage]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage, "supplier", supplier);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[WithMessage]");
        log.debug("Starting SupplyAsync[WithMessage] for sub-block: '{}' - Message: '{}'", subBlockName, subBlockStartMessage);
        return logger.supplyAsync(subBlockName, subBlockStartMessage, supplier);
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("SupplyAsync[WithType]", "subBlockName", subBlockName, "supplier", supplier, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[WithType]");
        log.debug("Starting SupplyAsync[WithType] for sub-block: '{}' - Type: {}", subBlockName, blockStartType);
        return logger.supplyAsync(subBlockName, supplier, blockStartType);
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        logMethodEntry("SupplyAsync[WithSerializer]", "subBlockName", subBlockName, "supplier", supplier, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[WithSerializer]");
        log.debug("Starting SupplyAsync[WithSerializer] for sub-block: '{}'", subBlockName);
        return logger.supplyAsync(subBlockName, supplier, endMessageSerializer);
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("SupplyAsync[MessageType]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "supplier", supplier, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[MessageType]");
        log.debug("Starting SupplyAsync[MessageType] for sub-block: '{}' - Message: '{}' - Type: {}",
                subBlockName, subBlockStartMessage, blockStartType);
        return logger.supplyAsync(subBlockName, subBlockStartMessage, supplier, blockStartType);
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        logMethodEntry("SupplyAsync[MessageSerializer]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage,
                "supplier", supplier, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[MessageSerializer]");
        log.debug("Starting SupplyAsync[MessageSerializer] for sub-block: '{}' - Message: '{}'", subBlockName, subBlockStartMessage);
        return logger.supplyAsync(subBlockName, subBlockStartMessage, supplier, endMessageSerializer);
    }

    public static <R> CompletableFuture<R> SupplyAsync(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        logMethodEntry("SupplyAsync[TypeSerializer]", "subBlockName", subBlockName, "supplier", supplier,
                "blockStartType", blockStartType, "endMessageSerializer", endMessageSerializer);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsync[TypeSerializer]");
        log.debug("Starting SupplyAsync[TypeSerializer] for sub-block: '{}' - Type: {}", subBlockName, blockStartType);
        return logger.supplyAsync(subBlockName, supplier, blockStartType, endMessageSerializer);
    }

    // ========== ASYNC SUPPLY WITH EXECUTOR METHOD OVERLOADS ==========

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType, Function<R, String> endMessageSerializer, Executor executor) {
        logMethodEntry("SupplyAsyncWith[Full]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[Full]");
        log.debug("Starting SupplyAsyncWith[Full] for sub-block: '{}' - Executor: {} - Type: {}",
                subBlockName, executor.getClass().getSimpleName(), subBlockStartType);
        return logger.supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier, Executor executor) {
        logMethodEntry("SupplyAsyncWith[Basic]", "subBlockName", subBlockName, "supplier", supplier, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[Basic]");
        log.debug("Starting SupplyAsyncWith[Basic] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, supplier, executor);
    }

    // Continue pattern for other SupplyAsyncWith methods...
    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, Executor executor) {
        logMethodEntry("SupplyAsyncWith[WithMessage]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[WithMessage]");
        log.debug("Starting SupplyAsyncWith[WithMessage] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Executor executor) {
        logMethodEntry("SupplyAsyncWith[WithType]", "subBlockName", subBlockName, "blockStartType", blockStartType, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[WithType]");
        log.debug("Starting SupplyAsyncWith[WithType] for sub-block: '{}' - Type: {} - Executor: {}",
                subBlockName, blockStartType, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, supplier, blockStartType, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer, Executor executor) {
        logMethodEntry("SupplyAsyncWith[WithSerializer]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[WithSerializer]");
        log.debug("Starting SupplyAsyncWith[WithSerializer] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, supplier, endMessageSerializer, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Executor executor) {
        logMethodEntry("SupplyAsyncWith[MessageType]", "subBlockName", subBlockName, "blockStartType", blockStartType, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[MessageType]");
        log.debug("Starting SupplyAsyncWith[MessageType] for sub-block: '{}' - Type: {} - Executor: {}",
                subBlockName, blockStartType, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, blockStartType, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, Function<R, String> endMessageSerializer, Executor executor) {
        logMethodEntry("SupplyAsyncWith[MessageSerializer]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[MessageSerializer]");
        log.debug("Starting SupplyAsyncWith[MessageSerializer] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, subBlockStartMessage, supplier, endMessageSerializer, executor);
    }

    public static <R> CompletableFuture<R> SupplyAsyncWith(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer, Executor executor) {
        logMethodEntry("SupplyAsyncWith[TypeSerializer]", "subBlockName", subBlockName, "blockStartType", blockStartType, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("SupplyAsyncWith[TypeSerializer]");
        log.debug("Starting SupplyAsyncWith[TypeSerializer] for sub-block: '{}' - Type: {} - Executor: {}",
                subBlockName, blockStartType, executor.getClass().getSimpleName());
        return logger.supplyAsyncWith(subBlockName, supplier, blockStartType, endMessageSerializer, executor);
    }

    // ========== ASYNC RUN METHOD OVERLOADS ==========

    public static CompletableFuture<Void> RunAsync(String subBlockName, String subBlockStartMessage, Runnable runnable, LogTypeBlockStartEnum subBlockStartType) {
        logMethodEntry("RunAsync[Full]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage, "subBlockStartType", subBlockStartType);
        ThreadVFL logger = getLoggerWithValidation("RunAsync[Full]");
        log.debug("Starting RunAsync[Full] for sub-block: '{}' - Message: '{}' - Type: {}",
                subBlockName, subBlockStartMessage, subBlockStartType);
        return logger.runAsync(subBlockName, subBlockStartMessage, runnable, subBlockStartType);
    }

    public static CompletableFuture<Void> RunAsync(String subBlockName, Runnable runnable) {
        logMethodEntry("RunAsync[Basic]", "subBlockName", subBlockName, "runnable", runnable);
        ThreadVFL logger = getLoggerWithValidation("RunAsync[Basic]");
        log.debug("Starting RunAsync[Basic] for sub-block: '{}'", subBlockName);
        return logger.runAsync(subBlockName, runnable);
    }

    public static CompletableFuture<Void> RunAsync(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        logMethodEntry("RunAsync[WithMessage]", "subBlockName", subBlockName, "subBlockStartMessage", subBlockStartMessage);
        ThreadVFL logger = getLoggerWithValidation("RunAsync[WithMessage]");
        log.debug("Starting RunAsync[WithMessage] for sub-block: '{}' - Message: '{}'", subBlockName, subBlockStartMessage);
        return logger.runAsync(subBlockName, subBlockStartMessage, runnable);
    }

    public static CompletableFuture<Void> RunAsync(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        logMethodEntry("RunAsync[WithType]", "subBlockName", subBlockName, "blockStartType", blockStartType);
        ThreadVFL logger = getLoggerWithValidation("RunAsync[WithType]");
        log.debug("Starting RunAsync[WithType] for sub-block: '{}' - Type: {}", subBlockName, blockStartType);
        return logger.runAsync(subBlockName, runnable, blockStartType);
    }

    // ========== ASYNC RUN WITH EXECUTOR METHOD OVERLOADS ==========

    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, String subBlockStartMessage, Runnable runnable, LogTypeBlockStartEnum subBlockStartType, Executor executor) {
        logMethodEntry("RunAsyncWith[Full]", "subBlockName", subBlockName, "subBlockStartType", subBlockStartType, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("RunAsyncWith[Full]");
        log.debug("Starting RunAsyncWith[Full] for sub-block: '{}' - Type: {} - Executor: {}",
                subBlockName, subBlockStartType, executor.getClass().getSimpleName());
        return logger.runAsyncWith(subBlockName, subBlockStartMessage, runnable, subBlockStartType, executor);
    }

    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, Runnable runnable, Executor executor) {
        logMethodEntry("RunAsyncWith[Basic]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("RunAsyncWith[Basic]");
        log.debug("Starting RunAsyncWith[Basic] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.runAsyncWith(subBlockName, runnable, executor);
    }

    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, String subBlockStartMessage, Runnable runnable, Executor executor) {
        logMethodEntry("RunAsyncWith[WithMessage]", "subBlockName", subBlockName, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("RunAsyncWith[WithMessage]");
        log.debug("Starting RunAsyncWith[WithMessage] for sub-block: '{}' - Executor: {}",
                subBlockName, executor.getClass().getSimpleName());
        return logger.runAsyncWith(subBlockName, subBlockStartMessage, runnable, executor);
    }

    public static CompletableFuture<Void> RunAsyncWith(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType, Executor executor) {
        logMethodEntry("RunAsyncWith[WithType]", "subBlockName", subBlockName, "blockStartType", blockStartType, "executor", executor);
        ThreadVFL logger = getLoggerWithValidation("RunAsyncWith[WithType]");
        log.debug("Starting RunAsyncWith[WithType] for sub-block: '{}' - Type: {} - Executor: {}",
                subBlockName, blockStartType, executor.getClass().getSimpleName());
        return logger.runAsyncWith(subBlockName, runnable, blockStartType, executor);
    }

    // ========== EVENT PUBLISHER METHODS ==========

    public static EventPublisherBlock CreateEventPublisherBlock(String eventBranchName, String publishStartMessage) {
        logMethodEntry("CreateEventPublisherBlock[WithMessage]", "eventBranchName", eventBranchName, "publishStartMessage", publishStartMessage);
        ThreadVFL logger = getLoggerWithValidation("CreateEventPublisherBlock");
        log.debug("Creating event publisher block for branch: '{}' - Start message: '{}'", eventBranchName, publishStartMessage);
        try {
            EventPublisherBlock block = logger.createEventPublisherBlock(eventBranchName, publishStartMessage);
            log.debug("Event publisher block created successfully for branch: '{}' - Block ID: {}",
                    eventBranchName, "present");
            return block;
        } catch (Exception e) {
            log.error("Failed to create event publisher block for branch '{}': {} - Exception type: {}",
                    eventBranchName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    public static EventPublisherBlock CreateEventPublisherBlock(String eventBranchName) {
        logMethodEntry("CreateEventPublisherBlock[Basic]", "eventBranchName", eventBranchName);
        ThreadVFL logger = getLoggerWithValidation("CreateEventPublisherBlock");
        log.debug("Creating basic event publisher block for branch: '{}'", eventBranchName);
        try {
            EventPublisherBlock block = logger.createEventPublisherBlock(eventBranchName, null);
            log.debug("Basic event publisher block created successfully for branch: '{}' - Block ID: {}",
                    eventBranchName, "present");
            return block;
        } catch (Exception e) {
            log.error("Failed to create basic event publisher block for branch '{}': {} - Exception type: {}",
                    eventBranchName, e.getMessage(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }
}
