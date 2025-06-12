package dev.kuku.vfl;

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
    private String lastLogId;

    public VisFlowLogger(VflBlockDataType block, VisFlowLogBuffer buffer) {
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
        String logId = UUID.randomUUID().toString();
        VflLogDataType log = new VflLogDataType(logId, block.getId(), lastLogId, VflLogType.MESSAGE, message, null, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(log);
        if (continueSequentially) {
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
        String[] blockInfo = setupSubBlock(subProcessName, null, VflLogType.SUB_BLOCK_START, continueSequentially);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subProcessName);
        T result = operation.apply(new VisFlowLogger(subBlock, buffer));

        logSubBlockCompletion(subProcessName, null, endMessage, result, subBlockId, preLogId);
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
        String[] blockInfo = setupSubBlock(subProcessName, null, VflLogType.SUB_BLOCK_START, continueSequentially);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subProcessName);
        operation.accept(new VisFlowLogger(subBlock, buffer));

        logSubBlockCompletion(subProcessName, null, endMessage, null, subBlockId, preLogId);
    }

    /**
     * Create a separate logger for parallel operations.
     * Use this when you want multiple operations to branch from the same point in your diagram.
     *
     * @param subProcessName name for this parallel sub-process
     * @return a new logger for tracking this parallel operation
     */
    public VisFlowLogger createParallelSubBlock(String subProcessName) {
        String[] blockInfo = setupSubBlock(subProcessName, null, VflLogType.SUB_BLOCK_START, false);
        String subBlockId = blockInfo[1];
        return new VisFlowLogger(new VflBlockDataType(this.block.getId(), subBlockId, subProcessName), buffer);
    }

    /**
     * Internal method to handle sub-process completion logging.
     */
    private <T> void logSubBlockCompletion(String blockName, String preFnMessage, Function<T, String> postFnMessage, T result, String subBlockId, String preLogId) {
        if (postFnMessage != null) {
            try {
                String postFnMessageResult = postFnMessage.apply(result);
                if (postFnMessageResult != null) {
                    String postLogId = UUID.randomUUID().toString();
                    VflLogDataType postLog = new VflLogDataType(postLogId, block.getId(), lastLogId, VflLogType.SUB_BLOCK_END, postFnMessageResult, Set.of(subBlockId), Instant.now().toEpochMilli());
                    buffer.pushLogToBuffer(postLog);
                }
            } catch (Exception e) {
                VflLogDataType exceptionLog = new VflLogDataType(UUID.randomUUID().toString(), this.block.getId(), preLogId, VflLogType.SUB_BLOCK_EXCEPTION, e.getMessage(), Set.of(subBlockId), Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(exceptionLog);
            }
        }
    }

    /**
     * Internal method to set up the infrastructure for a new sub-process.
     */
    private String[] setupSubBlock(String blockName, String preFnMessage, VflLogType logType, boolean continueSequentially) {
        String preLogId = UUID.randomUUID().toString();
        String subBlockId = UUID.randomUUID().toString();
        VflLogDataType preLog = new VflLogDataType(preLogId, block.getId(), lastLogId, logType, preFnMessage, Set.of(subBlockId), Instant.now().toEpochMilli());
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        buffer.pushBlockToBuffer(subBlock);
        buffer.pushLogToBuffer(preLog);
        if (continueSequentially) {
            lastLogId = preLogId;
        }
        return new String[]{preLogId, subBlockId};
    }
}