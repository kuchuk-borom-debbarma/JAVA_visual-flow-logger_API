package dev.kuku.vfl;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Logger for a block. Holds functions to log, create sub blocks and branch offs.
 */
public class VisFlowLogger {
    /// Represents the logger's current block
    private final VflBlockDataType block;
    private final VisFlowLogBuffer buffer;
    private String lastLogId;

    public VisFlowLogger(VflBlockDataType block, VisFlowLogBuffer buffer) {
        this.block = block;
        this.buffer = buffer;
    }

    /**
     * Log a simple message
     *
     * @param message message to log
     */
    public void log(String message) {
        String logId = UUID.randomUUID().toString();
        VflLogDataType log = new VflLogDataType(logId, block.getId(), lastLogId, VflLogType.MESSAGE, message, null, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(log);
        lastLogId = logId;
    }

    /**
     * create and run a sub-block which will have it's own sub-block logger. Logging in the sub-block needs to be done with the passed VisFlowLogger instance
     *
     * @param subBlockName  name of the sub-block
     * @param preFnMessage  message to log before the sub-block
     * @param postFnMessage message to log after the sub-block has finished operating.
     * @param fn            function to execute in the sub-block. It will be supplied it's own logger which has to be used to log operations in the function.
     * @param <T>           returned value of the function
     */
    public <T> void log(String subBlockName, String preFnMessage, Function<T, String> postFnMessage, Function<VisFlowLogger, T> fn) {
        String[] blockInfo = setupSubBlock(subBlockName, preFnMessage, VflLogType.SUB_BLOCK_START, true);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subBlockName);
        T result = fn.apply(new VisFlowLogger(subBlock, buffer));

        logWithBlock(subBlockName, preFnMessage, postFnMessage, result, subBlockId, preLogId);
    }

    /**
     * create and run a sub-block which will have it's own sub-block logger. Logging in the sub-block needs to be done with the passed VisFlowLogger instance
     *
     * @param subBlockName  name of the sub-block
     * @param preFnMessage  message to log before the sub-block
     * @param postFnMessage message to log after the sub-block has finished operation.
     * @param fn            function to execute in the sub-block. It will be supplied it's own logger which needs to be used to log the function's operation.
     */
    public void log(String subBlockName, String preFnMessage, Function<Void, String> postFnMessage, Consumer<VisFlowLogger> fn) {
        String[] blockInfo = setupSubBlock(subBlockName, preFnMessage, VflLogType.SUB_BLOCK_START, true);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, subBlockName);
        fn.accept(new VisFlowLogger(subBlock, buffer));

        logWithBlock(subBlockName, preFnMessage, postFnMessage, null, subBlockId, preLogId);
    }

    /**
     * Create a branch off logger which be branching off of this current logger. Useful for representing events, async operations, fire-and-forget functions etc.
     *
     * @param branchName name of the branch
     * @param message    message to show for the branch block.
     * @return {@link VisFlowLogger} branching VisFlowLogger
     */
    public VisFlowLogger branchOff(String branchName, String message) {
        //Do not update latestLog for branch off as it's starting a new branch and the next log should not have the branch log as it's parent.
        String[] blockInfo = setupSubBlock(branchName, message, VflLogType.BRANCH, false);
        String preLogId = blockInfo[0];
        String branchBlockId = blockInfo[1];

        VflBlockDataType branchBlock = new VflBlockDataType(block.getId(), branchBlockId, branchName);
        return new VisFlowLogger(branchBlock, buffer);
    }

    private <T> void logWithBlock(String blockName, String preFnMessage, Function<T, String> postFnMessage, T result, String subBlockId, String preLogId) {
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

    private String[] setupSubBlock(String blockName, String preFnMessage, VflLogType logType, boolean updateLatestLog) {
        String preLogId = UUID.randomUUID().toString();
        String subBlockId = UUID.randomUUID().toString();
        VflLogDataType preLog = new VflLogDataType(preLogId, block.getId(), lastLogId, logType, preFnMessage, Set.of(subBlockId), Instant.now().toEpochMilli());
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        buffer.pushBlockToBuffer(subBlock);
        buffer.pushLogToBuffer(preLog);
        if (updateLatestLog) {
            lastLogId = preLogId;
        }
        return new String[]{preLogId, subBlockId};
    }


    //TODO showing async operation that joins back is left
}