package dev.kuku.vfl;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.VflBlockDataType;
import dev.kuku.vfl.models.VflLogDataType;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VFL {
    private final VFLBuffer buffer;
    private StartedVFL rootBlock;

    public VFL(VFLBuffer vflBuffer) {
        this.buffer = vflBuffer;
    }

    public <T> T startWithResult(String blockName, Function<StartedVFL, T> operation) {
        return executeInBlock(blockName, operation);
    }

    public void start(String blockName, Consumer<StartedVFL> operation) {
        executeInBlock(blockName, vfl -> {
            operation.accept(vfl);
            return null;
        });
    }

    private <T> T executeInBlock(String blockName, Function<StartedVFL, T> operation) {
        String rootBlockId = UUID.randomUUID().toString();
        T result;

        try {
            this.rootBlock = new StartedVFL(new VflBlockDataType(null, rootBlockId, blockName), this.buffer);
            result = operation.apply(rootBlock);
        } catch (RuntimeException e) {
            if (rootBlock != null) {
                rootBlock.log("Exception : " + e.getMessage());
            }
            throw e; // Re-throw to allow proper error handling upstream
        } finally {
            if (rootBlock != null) {
                finalizeBlock(rootBlockId);
            }
        }

        return result;
    }

    private void finalizeBlock(String rootBlockId) {
        var endingLog = new VflLogDataType(
                UUID.randomUUID().toString(),
                rootBlockId,
                null,
                VflLogType.SUB_BLOCK_END,
                null,
                Set.of(rootBlockId),
                Instant.now().toEpochMilli()
        );
        this.buffer.pushLogToBuffer(endingLog);
        this.buffer.flushAllAsync();
    }
}