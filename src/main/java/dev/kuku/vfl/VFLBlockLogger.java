package dev.kuku.vfl;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VFLBlockLogger {
    private final VFLBuffer buffer;
    private final BlockData blockData;
    private String currentLogId = null;

    public VFLBlockLogger(BlockData blockData, VFLBuffer buffer) {
        this.buffer = buffer;
        this.blockData = blockData;
    }

    /// Log a message and move forward or stay at the current place
    public void log(String message, VflLogType logType, boolean moveForward) {
        String logId = UUID.randomUUID().toString();
        var messageLog = new LogData(logId,
                blockData.getId(),
                currentLogId,
                logType,
                message,
                null,
                Instant.now().toEpochMilli());
        this.buffer.pushLogToBuffer(messageLog);
        if (moveForward) {
            currentLogId = logId;
        }
    }

    /**
     * Creates a new block and runs the passed function inside it. Once completed the block is closed.
     * In case of exception during function execution, the exception is added as log in the sub block.
     * In case of any other exception It is added as log to this current instance of BlockLogger.
     *
     * @param blockName   name of the sub-block that will be created
     * @param message     message to print for starting log
     * @param process     the process to execute
     * @param moveForward to move forward or not
     */
    public <T> T logSubProcess(String blockName,
                               String message,
                               Function<VFLBlockLogger, T> process,
                               boolean moveForward) {
        try {
            Objects.requireNonNull(process, "Process cannot be null");
            Objects.requireNonNull(blockName, "Block name cannot be null");
            String subBlockId = UUID.randomUUID().toString();
            String subBlockStartLogId = UUID.randomUUID().toString();
            //Create the sub-block and push it to buffer before we start execution
            BlockData subBlock = new BlockData(blockData.getId(),
                    subBlockId,
                    blockName);
            buffer.pushBlockToBuffer(subBlock);
            //Create a log of type BLOCK_START and push it to buffer
            LogData subBlockLog = new LogData(subBlockStartLogId,
                    blockData.getId(),
                    currentLogId,
                    VflLogType.BLOCK_START,
                    message,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(subBlockLog);
            //Move forward if desired before executing process
            if (moveForward) {
                currentLogId = subBlockStartLogId;
            }
            //Execute a process in a try catch. If an exception is thrown, add it as a log in its own block
            VFLBlockLogger subProcessBlockLogger = new VFLBlockLogger(subBlock, buffer);
            T result;
            try {
                result = process.apply(subProcessBlockLogger);
            } catch (Exception e) {
                //Store the exception as a log within the sub block

                //To show the exception as part of the flow we move forward.
                //TO show the exception as a side log we do not move forward.
                //TODO make this part of configuration and ability to pass explicitly.
                subProcessBlockLogger.log("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                //Add process end log to current block
                String endLogId = UUID.randomUUID().toString();
                var endLog = new LogData(endLogId,
                        blockData.getId(),
                        subBlockStartLogId, //points to log that started the subBlock
                        VflLogType.BLOCK_END,
                        null,
                        Set.of(subBlockId),
                        Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(endLog);
                throw e;
            }
            //Add process end log to current block
            String endLogId = UUID.randomUUID().toString();
            var endLog = new LogData(endLogId,
                    blockData.getId(),
                    subBlockStartLogId, //points to log that started the subBlock
                    VflLogType.BLOCK_END,
                    null,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(endLog);
            return result;
        } catch (Exception e) {
            //To show the exception as part of the flow we move forward.
            //TO show the exception as a side log we do not move forward.
            //TODO make this part of configuration and ability to pass explicitly.
            log("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
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
                    currentLogId,
                    VflLogType.BLOCK_START,
                    message,
                    Set.of(subBlockId),
                    Instant.now().toEpochMilli()
            );
            buffer.pushLogToBuffer(subBlockStartLog);
            if (moveForward) {
                currentLogId = subBlockStartLogId;
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
