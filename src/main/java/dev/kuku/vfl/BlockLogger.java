package dev.kuku.vfl;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlockLogger {
    private final VFLBuffer buffer;
    private final BlockData blockData;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final StringLogSettingFactory stringLogSettingFactory = new StringLogSettingFactory(this);
    private String currentLogId = null;

    public BlockLogger(BlockData blockData, VFLBuffer buffer) {
        this.buffer = buffer;
        this.blockData = blockData;
    }

    public void write(String message) {
        this.addMessageLog(message, VflLogType.MESSAGE, true);
    }

    public void error(String message) {
        this.addMessageLog(message, VflLogType.EXCEPTION, true);
    }

    public StringLogSettingFactory writer() {
        return this.stringLogSettingFactory;
    }

    /// Core Log function to log a message and move forward or stay at the current place
    public void addMessageLog(String message, VflLogType logType, boolean moveForward) {
        // Ensure start log is created only once, even with concurrent access
        ensureStartLogCreated();

        String logId = UUID.randomUUID().toString();
        var messageLog = new LogData(
                logId,
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
     * Creates a new block and runs the passed function inside it. Once completed, the block is closed.
     * In case of exception during function execution, the exception is added as log in the sub block.
     * In case of any other exception It is added as log to this current instance of BlockLogger.
     *
     * @param blockName   name of the sub-block that will be created
     * @param message     message to print for starting log
     * @param process     the process to execute
     * @param moveForward to move forward or not
     */
    public <T> T logWithResult(String blockName,
                               String message,
                               Function<T, String> endMessage,
                               Function<BlockLogger, T> process,
                               boolean moveForward) {
        try {
            Objects.requireNonNull(process, "Process cannot be null");
            Objects.requireNonNull(blockName, "Block name cannot be null");

            // Ensure start log is created only once
            ensureStartLogCreated();

            String subBlockId = UUID.randomUUID().toString();
            String subBlockStartLogId = UUID.randomUUID().toString();
            //Create the sub-block and push it to buffer before we start execution
            BlockData subBlock = new BlockData(
                    subBlockId,
                    blockData.getId(),
                    blockName);
            buffer.pushBlockToBuffer(subBlock);
            //Create a log of type BLOCK_START and push it to buffer
            LogData subBlockLog = new LogData(
                    subBlockStartLogId,
                    blockData.getId(),
                    currentLogId,
                    VflLogType.SUB_BLOCK_START,
                    message,
                    subBlockId,
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(subBlockLog);
            //Move forward if desired before executing process
            if (moveForward) {
                currentLogId = subBlockStartLogId;
            }
            //Execute a process in a try catch. If an exception is thrown, add it as a log in its own block
            BlockLogger subProcessBlockLogger = new BlockLogger(subBlock, buffer);
            T result;
            try {
                result = process.apply(subProcessBlockLogger);
            } catch (Exception e) {
                //Store the exception as a log within the sub-block and then rethrow the exception. This will help capture the exception at both block and caller block level

                //To show the exception as part of the flow we move forward.
                //TO show the exception as a side log we do not move forward.
                //TODO make this part of configuration and ability to pass explicitly.
                subProcessBlockLogger.addMessageLog("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                //Add process end log to current block
                String endLogId = UUID.randomUUID().toString();
                var endLog = new LogData(
                        endLogId,
                        subBlockId, //points to the block that was created as sub-block
                        subBlockStartLogId, //points to log that started the subBlock
                        VflLogType.BLOCK_END,
                        executeEndMessageFn(endMessage, null),
                        null,
                        Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(endLog);
                throw e;
            }
            //Add process end log to current block
            //TODO add end message support
            String endLogId = UUID.randomUUID().toString();
            var endLog = new LogData(
                    endLogId,
                    subBlockId, //points to the block that was created as sub-block
                    subBlockStartLogId, //points to log that started the subBlock
                    VflLogType.BLOCK_END,
                    executeEndMessageFn(endMessage, result),
                    null,
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(endLog);
            return result;
        } catch (Exception e) {
            //To show the exception as part of the flow we move forward.
            //TO show the exception as a side log we do not move forward.
            //TODO make this part of configuration and ability to pass explicitly.
            addMessageLog("Exception when trying to run sub-block operation" + e.getMessage(), VflLogType.EXCEPTION, true);
            throw e;
        }
    }

    public void logWithoutResult(String blockName, String message, String endMessage, Consumer<BlockLogger> process, boolean moveForward) {
        Function<BlockLogger, Void> a = blockLogger -> {
            process.accept(blockLogger);
            return null;
        };
        this.logWithResult(blockName, message, (_) -> endMessage, a, moveForward);
    }


    private void ensureStartLogCreated() {
        /*
         * ATOMICITY: compareAndSet() is an atomic operation - it executes as one indivisible unit
         * at the CPU level using Compare-And-Swap (CAS) instruction. This means the read-compare-write
         * happens in a single uninterruptible step.
         *
         * WHY WE NEED IT: Consider two threads calling log() simultaneously:
         *
         * WITHOUT atomicity (regular boolean):
         * Thread-A: reads isInitialized (false) → [interrupted]
         * Thread-B: reads isInitialized (false) → createStartLog() → set true
         * Thread-A: [resumes] createStartLog() → set true
         * Result: createStartLog() called TWICE ✗
         *
         * WITH atomicity (AtomicBoolean):
         * Thread-A: compareAndSet(false, true) → CPU atomically: read+compare+write → returns true
         * Thread-B: compareAndSet(false, true) → CPU sees value already true → returns false
         * Result: Only Thread-A executes createStartLog() ✓
         *
         * BENEFITS IN OUR CASE:
         * - Block start time accurately reflects when FIRST log occurred
         * - No duplicate BLOCK_START entries in buffer
         * - Thread-safe without blocking (lock-free)
         * - Losing threads continue immediately without waiting
         */
        if (isInitialized.compareAndSet(false, true)) {
            createStartLog();
        }
    }

    private <T> String executeEndMessageFn(Function<T, String> endMessage, T result) {
        try {
            String msg = endMessage.apply(result);
            if (msg == null) {
                return "Processed end message is null";
            }
            return msg;
        } catch (Exception e) {
            return "Error when processing End Message : " + e.getMessage();
        }
    }

    private void createStartLog() {
        String startLogId = UUID.randomUUID().toString();
        LogData blockStartLog = new LogData(
                startLogId,
                blockData.getId(),
                null,
                VflLogType.BLOCK_START,
                null,
                null,
                Instant.now().toEpochMilli()
        );
        buffer.pushLogToBuffer(blockStartLog);
    }
}