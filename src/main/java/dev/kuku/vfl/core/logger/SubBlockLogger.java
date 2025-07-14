package dev.kuku.vfl.core.logger;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.VflLogType;

import java.util.UUID;

/**
 * A Block logger child class that can't be closed automatically and thus needs to be closed manually by calling close method.
 * Recommended to use with try-with-resource or close manually.
 */
public class SubBlockLogger extends BlockLogger implements AutoCloseable {
    private final BlockData parentBlockData;
    private String endMessage;

    public SubBlockLogger(BlockData parentBlockData, BlockData blockData, VFLBuffer buffer) {
        super(blockData, buffer);
        this.parentBlockData = parentBlockData;
    }

    public void setEndMessage(String endMessage) {
        this.endMessage = endMessage;
    }

    @Override
    public void close() {
        this.internalCoreLogger.createLogDataAndPush(UUID.randomUUID().toString(),
                this.internalCoreLogger.blockData.getId(),
                null,
                VflLogType.BLOCK_END,
                endMessage, null);
    }
}
