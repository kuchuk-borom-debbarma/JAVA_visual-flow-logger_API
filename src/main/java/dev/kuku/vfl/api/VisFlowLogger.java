package dev.kuku.vfl.api;

import dev.kuku.vfl.api.models.VflLogType;
import dev.kuku.vfl.internal.VisFlowLogBuffer;
import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
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
    private final VflBlockDataType block;
    private final VisFlowLogBuffer buffer;
    Logger logger = LoggerFactory.getLogger(VisFlowLogger.class);
    private String lastLogId;

    public VisFlowLogger(VflBlockDataType block, VisFlowLogBuffer buffer) {
        logger.debug("Create new logger with block {} and buffer {}", block, buffer);
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
     * This creates a box in diagram showing this step happened.
     *
     * @param message what happened in this step (will appear in the diagram box)
     */
    public void log(String message) {
        logger.debug("logging message {}", message);
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
        logger.debug("logging message {} with continueSequentially {}", message, continueSequentially);
        logger.debug("Generating log ID");
        String logId = UUID.randomUUID().toString();
        logger.debug("logId {}", logId);
        VflLogDataType log = new VflLogDataType(logId, block.getId(), lastLogId, VflLogType.MESSAGE, message, null, Instant.now().toEpochMilli());
        logger.debug("Created log = {}", log);
        logger.debug("Pushing log to buffer");
        buffer.pushLogToBuffer(log);
        if (continueSequentially) {
            logger.debug("Setting lastLogId from {} to {}", lastLogId, logId);
            lastLogId = logId;
        }
    }

    /**
     * Add a subprocess to the diagram that can be expanded/collapsed and returns a result.
     * This creates a nested section in the diagram showing the sub-process steps.
     *
     * @param subProcessName name for this sub-process (appears as the sub-process title)
     * @param operation      the work to do in this subprocess - you get a logger to track steps inside
     * @param endMessage     function that creates the completion message from the result
     * @param <T>            the type of result your sub-process returns
     */
    public <T> T logWithResult(String subProcessName, Function<VisFlowLogger, T> operation, Function<T, String> endMessage) {
        logger.debug("logWithResult subProcessName {} operation {} endMessage {}", subProcessName, operation, endMessage);
        return logWithResult(subProcessName, operation, endMessage, true);
    }

    /**
     * Add a sub-process to your diagram with control over the flow direction and returns a result.
     *
     * @param subProcessName       name for this sub-process (appears as the sub-process title)
     * @param operation            the work to do in this sub-process - you get a logger to track steps inside
     * @param endMessage           function that creates the completion message from the result
     * @param continueSequentially true = next step follows this sub-process in sequence (normal flow)
     * @param <T>                  the type of result your sub-process returns
     */
    public <T> T logWithResult(String subProcessName, Function<VisFlowLogger, T> operation, Function<T, String> endMessage, boolean continueSequentially) {
        logger.debug("logWithResult subProcessName {} operation {} endMessage {} continueSequentially {}", subProcessName, operation, endMessage, continueSequentially);
        logger.debug("Setting up sub block");
        String[] blockInfo = setupSubBlock(subProcessName, VflLogType.SUB_BLOCK_START, continueSequentially);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];
        logger.debug("setup complete. preLogId = {}, subBlockId = {}", preLogId, subBlockId);
        logger.debug("Creating sub block object");
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subProcessName);
        logger.debug("Created subBlock = {}", subBlock);
        logger.debug("Calling operation with buffer {}", buffer);
        T result = operation.apply(new VisFlowLogger(subBlock, buffer));
        logger.debug("Operation complete. result = {}", result);
        logger.debug("Creating log for end of sub-process");
        logSubBlockCompletion(endMessage, result, subBlockId, preLogId);
        return result;
    }

    /**
     * Add a sub-process to your diagram that doesn't return a result.
     * This creates a nested section in your diagram showing the sub-process steps.
     *
     * @param subProcessName name for this sub-process (appears as the sub-process title)
     * @param operation      the work to do in this sub-process - you get a logger to track steps inside
     * @param endMessage     function that creates the completion message (can use null for no end message)
     */
    public void logSubProcess(String subProcessName, Consumer<VisFlowLogger> operation, Function<Void, String> endMessage) {
        logger.debug("logSubProcess subProcessName {} operation {} endMessage {}", subProcessName, operation, endMessage);
        logSubProcess(subProcessName, operation, endMessage, true);
    }

    /**
     * Add a sub-process to your diagram with control over the flow direction.
     *
     * @param subProcessName       name for this sub-process (appears as the sub-process title)
     * @param operation            the work to do in this sub-process - you get a logger to track steps inside
     * @param endMessage           function that creates the completion message (can use null for no end message)
     * @param continueSequentially true = next step follows this sub-process in sequence (normal flow)
     */
    public void logSubProcess(String subProcessName, Consumer<VisFlowLogger> operation, Function<Void, String> endMessage, boolean continueSequentially) {
        logger.debug("logSubProcess subProcessName {} operation {} endMessage {} continueSequentially {}", subProcessName, operation, endMessage, continueSequentially);
        logger.debug("Setting up sub block");
        String[] blockInfo = setupSubBlock(subProcessName, VflLogType.SUB_BLOCK_START, continueSequentially);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];
        logger.debug("setup complete. preLogId = {}, subBlockId = {}", preLogId, subBlockId);
        logger.debug("Creating subBlock object");

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subProcessName);
        logger.debug("Created subBlock = {}", subBlock);
        logger.debug("Calling operation with buffer {}", buffer);
        operation.accept(new VisFlowLogger(subBlock, buffer));
        logger.debug("Operation complete. Running logSubBlockCompletion with endMessage {} and subBlockId {}", endMessage, subBlockId);
        logSubBlockCompletion(endMessage, null, subBlockId, preLogId);
    }


    /**
     * Create a separate logger for branching operations. Good for representing Async operations, events, Fire-Forget operations.<br>
     * Use this when you want multiple operations to branch from the same point in your diagram. <br>
     *
     * @param subProcessName name for this parallel sub-process
     * @return a new logger for tracking this parallel operation
     */
    public VisFlowLogger createBranch(String subProcessName) {
        logger.debug("createBranch subProcessName {}", subProcessName);
        String[] blockInfo = setupSubBlock(subProcessName, VflLogType.SUB_BLOCK_START, false);
        logger.debug("setup complete. preLogId = {}, subBlockId = {}", blockInfo[0], blockInfo[1]);
        String subBlockId = blockInfo[1];
        return new VisFlowLogger(new VflBlockDataType(this.block.getId(), subBlockId, subProcessName), buffer);
    }


    /**
     * Internal method to handle sub-process completion logging. <br>
     * Create a new log with parentId set to the logID of the log that signified the start. and type set to BLOCK_END. <br>
     * Adds postFnMessage if valid.
     */
    private <T> void logSubBlockCompletion(Function<T, String> postFnMessage, T result, String subBlockId, String preLogId) {
        logger.debug("logSubBlockCompletion postFnMessage {} result {} subBlockId {} preLogId {}", postFnMessage, result, subBlockId, preLogId);
        try {
            String postFnMessageResult = null;
            if (postFnMessage != null) {
                logger.debug("postFnMessage is not null. Calling postFnMessage with result {}", result);
                postFnMessageResult = postFnMessage.apply(result);
                logger.debug("postFnMessageResult is {}", postFnMessageResult);
            }
            String postLogId = UUID.randomUUID().toString();
            logger.debug("Generated postLogId {}", postLogId);
            VflLogDataType postLog = new VflLogDataType(postLogId, block.getId(), preLogId, VflLogType.SUB_BLOCK_END, postFnMessageResult, Set.of(subBlockId), Instant.now().toEpochMilli());
            logger.debug("Created postLog = {}", postLog);
            logger.debug("Pushing postLog to buffer");
            buffer.pushLogToBuffer(postLog);
        } catch (Exception e) {
            logger.error("Exception occurred while logging sub-process completion. Setting up exception log", e);
            VflLogDataType exceptionLog = new VflLogDataType(UUID.randomUUID().toString(), block.getId(), preLogId, VflLogType.SUB_BLOCK_EXCEPTION, e.getMessage(), Set.of(subBlockId), Instant.now().toEpochMilli());
            logger.debug("Created exceptionLog = {}", exceptionLog);
            logger.debug("Pushing exceptionLog to buffer");
            buffer.pushLogToBuffer(exceptionLog);
        }

    }

    /**
     * Internal method to set up the infrastructure for a new sub-process.
     * <br>
     * Creates log stating the start of the block and adds both of them to buffer
     */
    private String[] setupSubBlock(String blockName, VflLogType logType, boolean continueSequentially) {
        logger.debug("setupSubBlock blockName {} logType {} continueSequentially {}", blockName, logType, continueSequentially);
        String preLogId = UUID.randomUUID().toString();
        logger.debug("Generated preLogId {}", preLogId);
        String subBlockId = UUID.randomUUID().toString();
        logger.debug("Generated subBlockId {}", subBlockId);
        VflLogDataType preLog = new VflLogDataType(preLogId, block.getId(), lastLogId, logType, null, Set.of(subBlockId), Instant.now().toEpochMilli());
        logger.debug("Created preLog = {}", preLog);
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        logger.debug("Created subBlock = {}", subBlock);
        logger.debug("Pushing subBlock to buffer");
        buffer.pushBlockToBuffer(subBlock);
        logger.debug("Pushing preLog to buffer");
        buffer.pushLogToBuffer(preLog);
        if (continueSequentially) {
            logger.debug("Setting lastLogId from {} to {}", lastLogId, preLogId);
            lastLogId = preLogId;
        }
        return new String[]{preLogId, subBlockId};
    }
}