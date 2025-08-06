package dev.kuku.vfl.impl.threadlocal.logger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.TrimId;

@Slf4j
public final class ThreadVFLRunner extends VFLCallableRunner {

    private static final ThreadVFLRunner INSTANCE = new ThreadVFLRunner();

    private ThreadVFLRunner() {
        log.debug("ThreadVFLRunner singleton instance created");
    }

    private static void validateStartVFLParameters(String blockName, VFLBuffer buffer, Object executable) {
        log.trace("Validating startVFL parameters - blockName: '{}', buffer: {}, executable: {}",
                blockName, buffer != null ? "present" : "null", executable != null ? "present" : "null");

        if (blockName == null || blockName.trim().isEmpty()) {
            log.error("Validation failed: Block name is null or empty");
            throw new IllegalArgumentException("Block name cannot be null or empty");
        }
        if (buffer == null) {
            log.error("Validation failed: VFL buffer is null");
            throw new IllegalArgumentException("VFL buffer cannot be null");
        }
        if (executable == null) {
            log.error("Validation failed: Function/Runnable is null");
            throw new IllegalArgumentException("Function/Runnable cannot be null");
        }

        log.trace("StartVFL parameter validation passed for block: '{}'", blockName);
    }

    private static void validateEventListenerParameters(
            String eventListenerName,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        log.trace("Validating event listener parameters - name: '{}', buffer: {}, eventBlock: {}, runnable: {}",
                eventListenerName,
                buffer != null ? "present" : "null",
                eventPublisherBlock != null ? "present" : "null",
                runnable != null ? "present" : "null");

        if (eventListenerName == null || eventListenerName.trim().isEmpty()) {
            log.error("Validation failed: Event listener name is null or empty");
            throw new IllegalArgumentException("Event listener name cannot be null or empty");
        }
        if (buffer == null) {
            log.error("Validation failed: VFL buffer is null for event listener '{}'", eventListenerName);
            throw new IllegalArgumentException("VFL buffer cannot be null");
        }
        if (eventPublisherBlock == null) {
            log.error("Validation failed: Event publisher block is null for event listener '{}'", eventListenerName);
            throw new IllegalArgumentException("Event publisher block cannot be null");
        }
        if (runnable == null) {
            log.error("Validation failed: Runnable is null for event listener '{}'", eventListenerName);
            throw new IllegalArgumentException("Runnable cannot be null");
        }

        log.trace("Event listener parameter validation passed for: '{}'", eventListenerName);
    }

    // ==================== STATIC METHODS ====================

    public static <R> R StartVFL(String blockName, VFLBuffer buffer, Supplier<R> function) {
        log.debug("Static StartVFL called for block: '{}' with Supplier", blockName);
        return INSTANCE.startVFL(blockName, buffer, function);
    }

    public static void StartVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        log.debug("Static StartVFL called for block: '{}' with Runnable", blockName);
        INSTANCE.startVFL(blockName, buffer, runnable);
    }

    public static void StartEventListenerLogger(
            String eventListenerName,
            String eventStartMessage,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        log.debug("Static StartEventListenerLogger called for: '{}' with message: '{}'",
                eventListenerName, eventStartMessage);
        INSTANCE.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventPublisherBlock, runnable);
    }

    // ==================== INSTANCE METHODS ====================

    public <R> R startVFL(String blockName, VFLBuffer buffer, Supplier<R> function) {
        String threadInfo = GetThreadInfo();
        log.info("Starting VFL block '{}' with Supplier {}", blockName, threadInfo);

        validateStartVFLParameters(blockName, buffer, function);

        try {
            log.debug("Executing parent startVFL for block '{}' {}", blockName, threadInfo);
            R result = super.startVFL(blockName, buffer, function);
            log.info("Successfully completed VFL block '{}' {} - Result type: {}",
                    blockName, threadInfo, result != null ? result.getClass().getSimpleName() : "null");
            return result;
        } catch (Exception e) {
            log.error("Failed to execute VFL block '{}' {}: {} - Exception type: {}",
                    blockName, threadInfo, e.getMessage(), e.getClass().getSimpleName(), e);
            throw new RuntimeException("VFL execution failed for block: " + blockName, e);
        }
    }

    public void startVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        String threadInfo = GetThreadInfo();
        log.info("Starting VFL block '{}' with Runnable {}", blockName, threadInfo);

        validateStartVFLParameters(blockName, buffer, runnable);

        try {
            log.debug("Executing parent startVFL for block '{}' {}", blockName, threadInfo);
            super.startVFL(blockName, buffer, runnable);
            log.info("Successfully completed VFL block '{}' {}", blockName, threadInfo);
        } catch (Exception e) {
            log.error("Failed to execute VFL block '{}' {}: {} - Exception type: {}",
                    blockName, threadInfo, e.getMessage(), e.getClass().getSimpleName(), e);
            throw new RuntimeException("VFL execution failed for block: " + blockName, e);
        }
    }

    public void startEventListenerLogger(
            String eventListenerName,
            String eventStartMessage,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        String threadInfo = GetThreadInfo();
        log.info("Starting event listener '{}' {} - Start message: '{}'",
                eventListenerName, threadInfo, eventStartMessage);

        validateEventListenerParameters(eventListenerName, buffer, eventPublisherBlock, runnable);

        try {
            log.debug("Executing parent startEventListenerLogger for '{}' {}", eventListenerName, threadInfo);
            super.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventPublisherBlock, runnable);
            log.info("Successfully completed event listener '{}' {}", eventListenerName, threadInfo);
        } catch (Exception e) {
            log.error("Failed to execute event listener '{}' {}: {} - Exception type: {}",
                    eventListenerName, threadInfo, e.getMessage(), e.getClass().getSimpleName(), e);
            throw new RuntimeException("Event listener execution failed: " + eventListenerName, e);
        }
    }

    @Override
    protected VFL createRootLogger(VFLBlockContext rootContext) {
        String threadInfo = GetThreadInfo();
        log.debug("Creating root logger - Context: {} {}",
                rootContext != null ? "present" : "null", threadInfo);

        if (rootContext == null) {
            log.error("Root context validation failed - context is null {}", threadInfo);
            throw new IllegalArgumentException("Root context cannot be null");
        }

        ThreadVFL rootLogger = new ThreadVFL(rootContext);
        String loggerId = TrimId(rootLogger.loggerContext.blockInfo.getId());

        // Check if there's already a stack for this thread
        Stack<ThreadVFL> existingStack = ThreadVFL.LOGGER_STACK.get();
        if (existingStack != null) {
            log.warn("Found existing logger stack for root creation {} - Stack size: {} - This might indicate a potential issue",
                    threadInfo, existingStack.size());
        }

        // Initialize the ThreadLocal logger stack for this thread
        Stack<ThreadVFL> loggerStack = new Stack<>();
        loggerStack.push(rootLogger);
        ThreadVFL.LOGGER_STACK.set(loggerStack);

        log.info("ROOT LOGGER CREATED: Logger ID '{}' {} - Stack initialized with size: {} - Block: '{}'",
                loggerId, threadInfo, loggerStack.size(),
                rootContext.blockInfo != null ? rootContext.blockInfo.getBlockName() : "unknown");

        return rootLogger;
    }

    @Override
    protected VFL createEventListenerLogger(VFLBlockContext eventListenerContext) {
        String threadInfo = GetThreadInfo();
        log.debug("Creating event listener logger - Context: {} {}",
                eventListenerContext != null ? "present" : "null", threadInfo);

        if (eventListenerContext == null) {
            log.error("Event listener context validation failed - context is null {}", threadInfo);
            throw new IllegalArgumentException("Event listener context cannot be null");
        }

        ThreadVFL eventLogger = new ThreadVFL(eventListenerContext);
        String loggerId = TrimId(eventLogger.loggerContext.blockInfo.getId());
        String blockName = eventListenerContext.blockInfo != null ?
                eventListenerContext.blockInfo.getBlockName() : "unknown";

        Stack<ThreadVFL> currentStack = ThreadVFL.LOGGER_STACK.get();

        if (currentStack == null) {
            // Cross-thread scenario: Create new stack for this thread
            log.info("CROSS-THREAD EVENT: No existing stack found - Creating new stack for event listener '{}' {} - Block: '{}'",
                    loggerId, threadInfo, blockName);

            Stack<ThreadVFL> newStack = new Stack<>();
            ThreadVFL.LOGGER_STACK.set(newStack);
            newStack.push(eventLogger);

            log.info("EVENT LOGGER CREATED (NEW STACK): Logger ID '{}' {} - Stack size: {} - Block: '{}'",
                    loggerId, threadInfo, newStack.size(), blockName);
        } else {
            // Same-thread scenario: Add to existing stack
            int previousStackSize = currentStack.size();
            String parentLoggerId = currentStack.isEmpty() ? "none" :
                    TrimId(currentStack.peek().loggerContext.blockInfo.getId());

            log.debug("SAME-THREAD EVENT: Adding to existing stack - Parent logger: '{}' - Stack size before: {}",
                    parentLoggerId, previousStackSize);

            currentStack.push(eventLogger);

            log.info("EVENT LOGGER CREATED (EXISTING STACK): Logger ID '{}' {} - Parent: '{}' - Stack size: {} -> {} - Block: '{}'",
                    loggerId, threadInfo, parentLoggerId, previousStackSize, currentStack.size(), blockName);
        }

        // Get the current logger (which should be our event logger)
        ThreadVFL currentLogger = ThreadVFL.getCurrentLogger();
        String currentLoggerId = currentLogger != null ?
                TrimId(currentLogger.loggerContext.blockInfo.getId()) : "null";

        log.debug("EVENT LOGGER SETUP COMPLETE: Current logger ID '{}' - Expected ID '{}' - Match: {}",
                currentLoggerId, loggerId, loggerId.equals(currentLoggerId));

        return currentLogger;
    }
}
