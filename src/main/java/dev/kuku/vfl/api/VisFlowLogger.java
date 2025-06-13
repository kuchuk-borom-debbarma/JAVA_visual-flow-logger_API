package dev.kuku.vfl.api;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import dev.kuku.vfl.api.models.VflLogType;
import dev.kuku.vfl.internal.VisFlowLogBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Visual Flow Logger is a logging framework that allows developers to visualize their logs and how they flow.
 */
public class VisFlowLogger {
    private static final Logger logger = LoggerFactory.getLogger(VisFlowLogger.class);

    private final VflBlockDataType block;
    private final VisFlowLogBuffer buffer;
    private String lastLogId;

    public VisFlowLogger(VflBlockDataType block, VisFlowLogBuffer buffer) {
        logger.debug("Creating new logger with block {} and buffer {}", block, buffer);
        this.block = block;
        this.buffer = buffer;
    }

    /**
     * @return Block that this logger instance represents
     */
    public VflBlockDataType getBlock() {
        return block;
    }

    /**
     * Add a step to the flow.
     * This creates a box in the diagram showing this step happened.
     *
     * @param message what happened in this step (will appear in the diagram box)
     */
    public void log(String message) {
        log(message, true);
    }

    /**
     * Add a step to the diagram with control over the flow direction.
     *
     * @param message              what happened in this step (will appear in the diagram box)
     * @param continueSequentially true = next step follows this one in sequence (normal flow)
     *                             false = next step branches from the same point as this one (parallel operations)
     */
    public void log(String message, boolean continueSequentially) {
        logger.debug("Logging message '{}' with continueSequentially={}", message, continueSequentially);

        String logId = generateLogId();
        VflLogDataType log = createLog(logId, VflLogType.MESSAGE, message, null);

        buffer.pushLogToBuffer(log);

        if (continueSequentially) {
            updateLastLogId(logId);
        }
    }

    /**
     * Add a subprocess to the diagram that can be expanded/collapsed and returns a result.
     * This creates a nested section in the diagram showing the subprocess steps.
     *
     * @param subProcessName name for this subprocess (appears as the subprocess title)
     * @param operation      the work to do in this subprocess - you get a logger to track steps inside
     * @param endMessage     function that creates the completion message from the result
     * @param <T>            the type of result your subprocess returns
     */
    public <T> T logWithResult(String subProcessName, Function<VisFlowLogger, T> operation, Function<T, String> endMessage) {
        return logWithResult(subProcessName, operation, endMessage, true);
    }

    /**
     * Add a subprocess to your diagram with control over the flow direction and returns a result.
     *
     * @param subProcessName       name for this subprocess (appears as the subprocess title)
     * @param operation            the work to do in this subprocess - you get a logger to track steps inside
     * @param endMessage           function that creates the completion message from the result
     * @param continueSequentially true = next step follows this subprocess in sequence (normal flow)
     * @param <T>                  the type of result your subprocess returns
     */
    public <T> T logWithResult(String subProcessName, Function<VisFlowLogger, T> operation, Function<T, String> endMessage, boolean continueSequentially) {
        logger.debug("Starting sub-process '{}' with result, continueSequentially={}", subProcessName, continueSequentially);

        SubBlockInfo subBlockInfo = startSubBlock(subProcessName, continueSequentially);
        VisFlowLogger subLogger = createSubLogger(subBlockInfo.blockId, subProcessName);

        try {
            T result = operation.apply(subLogger);
            logger.debug("Sub-process '{}' completed successfully with result: {}", subProcessName, result);
            endSubBlock(endMessage, result, subBlockInfo);
            return result;
        } catch (Exception e) {
            logger.error("Exception in sub-process '{}': {}", subProcessName, e.getMessage(), e);
            logSubBlockException(e, subBlockInfo);
            throw e;
        }
    }

    /**
     * Add a subprocess to your diagram that doesn't return a result.
     * This creates a nested section in your diagram showing the subprocess steps.
     *
     * @param subProcessName name for this `subprocess (appears as the subprocess title)
     * @param operation      the work to do in this subprocess - you get a logger to track steps inside
     * @param endMessage     function that creates the completion message (can use null for no end message)
     */
    public void logSubProcess(String subProcessName, Consumer<VisFlowLogger> operation, Function<Void, String> endMessage) {
        logSubProcess(subProcessName, operation, endMessage, true);
    }

    /**
     * Add a subprocess to your diagram with control over the flow direction.
     *
     * @param subProcessName       name for this subprocess (appears as the subprocess title)
     * @param operation            the work to do in this subprocess - you get a logger to track steps inside
     * @param endMessage           function that creates the completion message (can use null for no end message)
     * @param continueSequentially true = next step follows this subprocess in sequence (normal flow)
     */
    public void logSubProcess(String subProcessName, Consumer<VisFlowLogger> operation, Function<Void, String> endMessage, boolean continueSequentially) {
        logger.debug("Starting sub-process '{}', continueSequentially={}", subProcessName, continueSequentially);

        SubBlockInfo subBlockInfo = startSubBlock(subProcessName, continueSequentially);
        VisFlowLogger subLogger = createSubLogger(subBlockInfo.blockId, subProcessName);

        operation.accept(subLogger);
        logger.debug("Sub-process '{}' completed", subProcessName);
        endSubBlock(endMessage, null, subBlockInfo);
    }

    /**
     * Create a separate logger for branching operations. Good for representing Async operations, events, Fire-Forget operations.
     * Use this when you want multiple operations to branch from the same point in your diagram.
     *
     * @param subProcessName name for this parallel subprocess
     * @return a new logger for tracking this parallel operation
     */
    public VisFlowLogger createBranch(String subProcessName) {
        logger.debug("Creating branch for sub-process '{}'", subProcessName);

        SubBlockInfo subBlockInfo = startSubBlock(subProcessName, false);
        return createSubLogger(subBlockInfo.blockId, subProcessName);
    }

    // Private helper methods

    private String generateLogId() {
        String logId = UUID.randomUUID().toString();
        logger.debug("Generated logId: {}", logId);
        return logId;
    }

    private VflLogDataType createLog(String logId, VflLogType logType, String message, Set<String> blockIds) {
        VflLogDataType log = new VflLogDataType(logId, block.getId(), lastLogId, logType, message, blockIds, Instant.now().toEpochMilli());
        logger.debug("Created log: {}", log);
        return log;
    }

    private void updateLastLogId(String newLogId) {
        logger.debug("Updating lastLogId from '{}' to '{}'", lastLogId, newLogId);
        lastLogId = newLogId;
    }

    private SubBlockInfo startSubBlock(String blockName, boolean continueSequentially) {
        String preLogId = generateLogId();
        String subBlockId = generateLogId();

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        VflLogDataType preLog = createLog(preLogId, VflLogType.SUB_BLOCK_START, null, Set.of(subBlockId));

        buffer.pushBlockToBuffer(subBlock);
        buffer.pushLogToBuffer(preLog);

        if (continueSequentially) {
            updateLastLogId(preLogId);
        }

        return new SubBlockInfo(preLogId, subBlockId);
    }

    private VisFlowLogger createSubLogger(String subBlockId, String subProcessName) {
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subProcessName);
        return new VisFlowLogger(subBlock, buffer);
    }

    private <T> void endSubBlock(Function<T, String> endMessageFunction, T result, SubBlockInfo subBlockInfo) {
        if (endMessageFunction == null) {
            return;
        }

        try {
            String endMessage = endMessageFunction.apply(result);
            logger.debug("Sub-block end message: {}", endMessage);

            String postLogId = generateLogId();
            VflLogDataType endLog = createLog(postLogId, VflLogType.SUB_BLOCK_END, endMessage, Set.of(subBlockInfo.blockId));
            buffer.pushLogToBuffer(endLog);
        } catch (Exception e) {
            logger.error("Failed to create sub-block end message", e);
        }
    }

    private void logSubBlockException(Exception exception, SubBlockInfo subBlockInfo) {
        String exceptionLogId = generateLogId();
        VflLogDataType exceptionLog = createLog(exceptionLogId, VflLogType.SUB_BLOCK_EXCEPTION, exception.getMessage(), Set.of(subBlockInfo.blockId));
        buffer.pushLogToBuffer(exceptionLog);
    }

    // Helper class to hold subblock information
    private record SubBlockInfo(String preLogId, String blockId) {
    }
}