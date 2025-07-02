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
    private String currentLogId = null;

    public BlockLogger(BlockData blockData, VFLBuffer buffer) {
        this.buffer = buffer;
        this.blockData = blockData;
    }

    public LogBuilder<Void> log() {
        return LogBuilder.message(this);
    }

    public LogBuilder<Void> log(String message) {
        return LogBuilder.message(this).message(message);
    }

    public <T> LogBuilder<T> process(Function<BlockLogger, T> process) {
        return LogBuilder.process(this, process);
    }

    public LogBuilder<Void> process(Consumer<BlockLogger> process) {
        return LogBuilder.process(this, process);
    }

    public void addMessageLog(String message, VflLogType logType, boolean moveForward) {
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

    public <T> T logWithResult(String blockName,
                               String message,
                               Function<T, String> endMessage,
                               Function<BlockLogger, T> process,
                               boolean moveForward) {
        try {
            Objects.requireNonNull(process, "Process cannot be null");
            Objects.requireNonNull(blockName, "Block name cannot be null");

            ensureStartLogCreated();

            String subBlockId = UUID.randomUUID().toString();
            String subBlockStartLogId = UUID.randomUUID().toString();

            BlockData subBlock = new BlockData(
                    subBlockId,
                    blockData.getId(),
                    blockName);
            buffer.pushBlockToBuffer(subBlock);

            LogData subBlockLog = new LogData(
                    subBlockStartLogId,
                    blockData.getId(),
                    currentLogId,
                    VflLogType.SUB_BLOCK_START,
                    message,
                    subBlockId,
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(subBlockLog);

            if (moveForward) {
                currentLogId = subBlockStartLogId;
            }

            BlockLogger subProcessBlockLogger = new BlockLogger(subBlock, buffer);
            T result;
            try {
                result = process.apply(subProcessBlockLogger);
            } catch (Exception e) {
                subProcessBlockLogger.addMessageLog("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                String endLogId = UUID.randomUUID().toString();
                var endLog = new LogData(
                        endLogId,
                        subBlockId,
                        subBlockStartLogId,
                        VflLogType.BLOCK_END,
                        executeEndMessageFn(endMessage, null),
                        null,
                        Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(endLog);
                throw e;
            }

            String endLogId = UUID.randomUUID().toString();
            var endLog = new LogData(
                    endLogId,
                    subBlockId,
                    subBlockStartLogId,
                    VflLogType.BLOCK_END,
                    executeEndMessageFn(endMessage, result),
                    null,
                    Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(endLog);
            return result;
        } catch (Exception e) {
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
        if (isInitialized.compareAndSet(false, true)) {
            createStartLog();
        }
    }

    private <T> String executeEndMessageFn(Function<T, String> endMessage, T result) {
        if (endMessage == null) return null;
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

    public static class LogBuilder<T> {
        private final BlockLogger blockLogger;
        private String message;
        private Function<T, String> endMessage;
        private VflLogType logType = VflLogType.MESSAGE;
        private boolean moveForward = true;
        private Function<BlockLogger, T> process;
        private Consumer<BlockLogger> voidProcess;

        private LogBuilder(BlockLogger blockLogger) {
            this.blockLogger = blockLogger;
        }

        public static LogBuilder<Void> message(BlockLogger logger) {
            return new LogBuilder<>(logger);
        }

        public static <T> LogBuilder<T> process(BlockLogger logger, Function<BlockLogger, T> process) {
            LogBuilder<T> builder = new LogBuilder<>(logger);
            builder.process = process;
            return builder;
        }

        public static LogBuilder<Void> process(BlockLogger logger, Consumer<BlockLogger> process) {
            LogBuilder<Void> builder = new LogBuilder<>(logger);
            builder.voidProcess = process;
            return builder;
        }

        public LogBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public LogBuilder<T> endMessage(String endMessage) {
            this.endMessage = _ -> endMessage;
            return this;
        }

        public LogBuilder<T> endMessage(Function<T, String> endMessage) {
            this.endMessage = endMessage;
            return this;
        }

        public LogBuilder<T> type(VflLogType logType) {
            this.logType = logType;
            return this;
        }

        public LogBuilder<T> stay() {
            this.moveForward = false;
            return this;
        }

        public LogBuilder<T> moveForward() {
            this.moveForward = true;
            return this;
        }

        public LogBuilder<T> info() {
            return type(VflLogType.MESSAGE);
        }

        public LogBuilder<T> error() {
            return type(VflLogType.EXCEPTION);
        }

        // Terminal operations
        public void write() {
            if (message == null) {
                throw new IllegalStateException("Message must be set for write operations");
            }
            blockLogger.addMessageLog(message, logType, moveForward);
        }

        @SuppressWarnings("unchecked")
        public T execute(String blockName) {
            if (process == null && voidProcess == null) {
                throw new IllegalStateException("Process must be set for execution operations");
            }

            if (voidProcess != null) {
                Function<BlockLogger, Void> wrappedProcess = logger -> {
                    voidProcess.accept(logger);
                    return null;
                };
                return (T) blockLogger.logWithResult(blockName, message,
                        endMessage != null ? (Function<Void, String>) endMessage : null,
                        wrappedProcess, moveForward);
            }

            return blockLogger.logWithResult(blockName, message, endMessage, process, moveForward);
        }
    }
}