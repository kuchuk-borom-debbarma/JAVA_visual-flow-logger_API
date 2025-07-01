package dev.kuku.vfl;

import dev.kuku.VFLBlockLogger;
import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VFL {
    private final VFLBuffer buffer;
    private VFLBlockLogger rootBlockLogger;

    public VFL(VFLBuffer vflBuffer) {
        this.buffer = vflBuffer;
    }

    public <T> T startWithResult(String blockName, Function<VFLBlockLogger, T> operation) {
        return executeInBlock(blockName, operation);
    }

    public void start(String blockName, Consumer<VFLBlockLogger> operation) {
        executeInBlock(blockName, vfl -> {
            operation.accept(vfl);
            return null;
        });
    }

    private <T> T executeInBlock(String blockName, Function<VFLBlockLogger, T> operation) {
        String rootBlockId = UUID.randomUUID().toString();
        T result;
        try {
            //Create root block model
            var rootBlock = new BlockData(null, rootBlockId, blockName);
            //Push the newly created root block model to buffer
            this.buffer.pushBlockToBuffer(rootBlock);
            //Create blockLogger instance for rootBlock
            this.rootBlockLogger = new VFLBlockLogger(new BlockData(null, rootBlockId, blockName), this.buffer);
            //Execute the operation with blockLogger passed as argument
            result = operation.apply(rootBlockLogger);
        } catch (RuntimeException e) {
            //if exception is thrown, add it to blockLogger
            if (rootBlockLogger != null) {
                rootBlockLogger.log("Exception : " + e.getMessage(), VflLogType.EXCEPTION, true);
            }
            throw e; // Re-throw to allow proper error handling upstream
        } finally {
            //Finally, finalize the block after the operation is over
            if (rootBlockLogger != null) {
                finalizeBlock(rootBlockId);
            }
        }
        return result;
    }

    /// Add ending log to rootBlockLogger and flushes everything
    private void finalizeBlock(String rootBlockId) {
        var endingLog = new LogData(
                UUID.randomUUID().toString(),
                rootBlockId,
                null,
                VflLogType.EXCEPTION,
                null,
                Set.of(rootBlockId),
                Instant.now().toEpochMilli()
        );
        this.buffer.pushLogToBuffer(endingLog);
        this.buffer.flushAllAsync();
    }
}