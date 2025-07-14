package dev.kuku.vfl;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VFL {
    private final VFLBuffer buffer;
    private BlockLogger rootBlockLogger;

    public VFL(VFLBuffer vflBuffer) {
        this.buffer = vflBuffer;
    }

    public <T> T start(String blockName, Function<BlockLogger, T> operation) {
        return executeInBlock(blockName, operation);
    }

    public void start(String blockName, Consumer<BlockLogger> operation) {
        executeInBlock(blockName, vfl -> {
            operation.accept(vfl);
            return null;
        });
    }

    private <T> T executeInBlock(String blockName, Function<BlockLogger, T> operation) {
        String rootBlockId = UUID.randomUUID().toString();
        T result;
        try {
            //Create root block model
            BlockData rootBlock = new BlockData(rootBlockId, null, blockName);
            //Push the newly created root block model to buffer
            this.buffer.pushBlockToBuffer(rootBlock);
            //Create blockLogger instance for rootBlock
            this.rootBlockLogger = new BlockLogger(new BlockData(rootBlockId, null, blockName), this.buffer);
            //Execute the operation with blockLogger passed as argument
            result = operation.apply(rootBlockLogger);
        } catch (Exception e) {
            //if an exception is thrown, add it to blockLogger
            if (rootBlockLogger != null) {
                rootBlockLogger.error("Exception: " + e.getClass().getSimpleName() +
                        (e.getMessage() != null ? " - " + e.getMessage() : ""));
            }
            if (rootBlockLogger != null) {
                finalizeBlock(rootBlockId);
            }
            throw e; // Re-throw to allow proper error handling upstream
        }
        if (rootBlockLogger != null) {
            finalizeBlock(rootBlockId);
        }
        return result;
    }

    /// Add ending log to rootBlockLogger and flushes everything
    private void finalizeBlock(String rootBlockId) {
        LogData endingLog = new LogData(
                UUID.randomUUID().toString(),
                rootBlockId,
                null,
                VflLogType.BLOCK_END,
                null,
                rootBlockId,
                Instant.now().toEpochMilli()
        );
        this.buffer.pushLogToBuffer(endingLog);
        this.buffer.shutdown();
    }
}
//TODO try with resource design support