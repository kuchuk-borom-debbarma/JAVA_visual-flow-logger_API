package dev.kuku;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VFLBlockLogger {
    private final VFLBuffer buffer;
    private final BlockData blockData;
    private String currentLog = null;

    public VFLBlockLogger(BlockData blockData, VFLBuffer buffer) {
        this.buffer = buffer;
        this.blockData = blockData;
    }

    /// Log a message and move forward or stay at the current place
    public void log(String message, VflLogType logType, boolean moveForward) {
        String logId = UUID.randomUUID().toString();
        var messageLog = new LogData(logId,
                blockData.getId(),
                currentLog,
                logType,
                message,
                null,
                Instant.now().toEpochMilli());
        this.buffer.pushLogToBuffer(messageLog);
        if (moveForward) {
            currentLog = logId;
        }
    }

    /**
     * Log a nested sub process contained within a nested sub-block.
     *
     * @param blockName   name of the block where the sub-process will take place
     * @param process     process to execute
     * @param moveForward whether to move forward the flow or stay at the same place
     * @return result of the process execution
     */
    public <T> T logSubProcess(String blockName, String message, Function<T, String> postExecutionMessageFn, Function<VFLBlockLogger, T> process, boolean moveForward) {
        String subBlockId = UUID.randomUUID().toString();
        String subBlockStartLogId = UUID.randomUUID().toString();
        try {
            //Create the sub-block and push it to buffer before we start execution
            BlockData subBlock = new BlockData(blockData.getId(),
                    subBlockId,
                    blockName);
            buffer.pushBlockToBuffer(subBlock);
            //Create a log of type BLOCK_START and push it to buffer
            LogData subBlockLog = new LogData(subBlockStartLogId,
                    blockData.getId(),
                    currentLog,
                    VflLogType.BLOCK_START,
                    message,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(subBlockLog);
            //Move forward if desired before executing process
            if (moveForward) {
                currentLog = subBlockStartLogId;
            }
            //Execute process in a try catch. If an exception is thrown, add it as a log in its own block
            VFLBlockLogger subProcessBlockLogger = new VFLBlockLogger(subBlock, buffer);
            T result;
            try {
                result = process.apply(subProcessBlockLogger);
            } catch (Exception e) {
                subProcessBlockLogger.log("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                throw e;
            }
            //After executing the process, end the sub-block where the sub-process executed.
            //TODO reduce code repetition
            String postExecutionMsg;
            //If there is a post-Execution message then we execute it and add it as ending log's message
            if (postExecutionMessageFn != null) {
                try {
                    postExecutionMsg = postExecutionMessageFn.apply(result);
                    String endLogId = UUID.randomUUID().toString();
                    LogData endLog = new LogData(endLogId,
                            blockData.getId(),
                            subBlockStartLogId, VflLogType.BLOCK_END,
                            postExecutionMsg,
                            Set.of(subBlockId),
                            Instant.now().toEpochMilli());
                    buffer.pushLogToBuffer(endLog);
                } catch (Exception e) {
                    String endLogId = UUID.randomUUID().toString();
                    LogData endLog = new LogData(endLogId,
                            blockData.getId(),
                            subBlockStartLogId, VflLogType.BLOCK_END,
                            null,
                            Set.of(subBlockId),
                            Instant.now().toEpochMilli());
                    buffer.pushLogToBuffer(endLog);
                    throw e;
                }
            } else {
                String endLogId = UUID.randomUUID().toString();
                LogData endLog = new LogData(endLogId,
                        blockData.getId(),
                        subBlockStartLogId, VflLogType.BLOCK_END,
                        null,
                        Set.of(subBlockId),
                        Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(endLog);
            }

            return result;
        } catch (Exception e) {
            //If something went wrong at any stage add it as log to this block
            //TODO exception log type
            this.log("Exception when attempting to execute sub-process\n" + e.getMessage(), VflLogType.EXCEPTION, moveForward);
            throw e;
        }

    }

    public void logSubProcess(String blockName, String message, boolean moveForward, Consumer<VFLBlockLogger> process) {
        String subBlockId = UUID.randomUUID().toString();
        String subBlockStartLogId = UUID.randomUUID().toString();
        try {
            BlockData subBlock = new BlockData(blockData.getId(),
                    subBlockId, blockName);
            buffer.pushBlockToBuffer(subBlock);
            LogData subBlockStartLog = new LogData(
                    subBlockStartLogId,
                    blockData.getId(),
                    currentLog,
                    VflLogType.BLOCK_START,
                    message,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli()
            );
            buffer.pushLogToBuffer(subBlockStartLog);
            if (moveForward) {
                currentLog = subBlockStartLogId;
            }
            VFLBlockLogger subBlockLogger = new VFLBlockLogger(subBlock, buffer);
            try {
                process.accept(subBlockLogger);
            } catch (Exception e) {
                subBlockLogger.log("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                throw e;
            }
            String subBlockEndLogId = UUID.randomUUID().toString();
            LogData endingLog = new LogData(subBlockEndLogId,
                    blockData.getId(),
                    subBlockStartLogId,
                    VflLogType.BLOCK_END,
                    null,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(endingLog);
        } catch (Exception e) {
            log("Exception " + e.getMessage(), VflLogType.EXCEPTION, moveForward);
            throw e;
        }
    }
}
