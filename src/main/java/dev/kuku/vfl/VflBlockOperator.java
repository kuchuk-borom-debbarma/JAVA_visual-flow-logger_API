package dev.kuku.vfl;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class VflBlockOperator {
    private final String blockId;
    private final VflBuffer buffer;
    private final String latestLogId;
    private volatile boolean used = false;

    public VflBlockOperator(String blockId, VflBuffer buffer) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = null;
    }

    public VflBlockOperator(String blockId, VflBuffer buffer, String latestLogId) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = latestLogId;
    }

    /**
     * Logs a message and returns a new VflBlockOperator with updated latestLogId
     */
    public VflBlockOperator log(String message) {
        checkAndMarkUsed();
        String logId = UUID.randomUUID().toString();
        var log = buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.MESSAGE, message, null);
        return new VflBlockOperator(blockId, buffer, log.getId());
    }

    /**
     * For operations that return a value
     */
    public <T> T log(
            Function<VflBlockOperator, T> fn,
            String preBlockMessage,
            Function<T, String> postBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();
        String endLogId = UUID.randomUUID().toString();

        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(subBlockId));

        T result = fn.apply(buffer.createBlock(this.blockId, subBlockId, blockName, endLogId));

        buffer.createLog(endLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_END, postBlockMessage.apply(result), Set.of(subBlockId));

        return result;
    }

    /**
     * For void operations
     */
    public void log(
            Consumer<VflBlockOperator> fn,
            String preBlockMessage,
            Supplier<String> postBlockMessage,
            String blockName
    ) {
        log(
                v -> {
                    fn.accept(v);
                    return null;
                },
                preBlockMessage,
                o -> postBlockMessage.get(),
                blockName
        );
    }

    private void checkAndMarkUsed() {
        if (used) {
            throw new IllegalStateException(
                    "VflBlockOperator instance has already been used. " +
                            "Use the returned instance from previous operations."
            );
        }
        used = true;
    }
}