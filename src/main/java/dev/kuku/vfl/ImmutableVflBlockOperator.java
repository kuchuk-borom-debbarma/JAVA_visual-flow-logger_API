package dev.kuku.vfl;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


public final class ImmutableVflBlockOperator {
    private final String blockId;
    private final VflBuffer buffer;
    private final String latestLogId; // Final field - immutable!

    public ImmutableVflBlockOperator(String blockId, VflBuffer buffer) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = null;
    }

    public ImmutableVflBlockOperator(String blockId, VflBuffer buffer, String latestLogId) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = latestLogId;
    }

    /**
     * Add a log and return NEW instance with updated latestLogId
     */
    public ImmutableVflBlockOperator log(String message) {
        String logId = UUID.randomUUID().toString();
        var log = buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.MESSAGE, message, null);
        // Return NEW instance - no mutation!
        return new ImmutableVflBlockOperator(this.blockId, this.buffer, log.getId());
    }

    /**
     * Execute block and return NEW instance with updated chain
     */
    public <T> BlockResult<T> log(
            Function<ImmutableVflBlockOperator, BlockResult<T>> fn,
            String preBlockMessage,
            Function<T, String> postBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();

        // Create sub-block
        ImmutableVflBlockOperator block = buffer.createBlock(this.blockId, subBlockId, blockName);

        // Create START log
        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(subBlockId));

        // Execute function
        BlockResult<T> result = fn.apply(block);

        // Create END log
        String endLogId = UUID.randomUUID().toString();
        buffer.createLog(endLogId, this.blockId, startLogId,
                VflLogType.END, postBlockMessage.apply(result.value()), Set.of(subBlockId));

        // Return result with NEW parent instance
        ImmutableVflBlockOperator newParent = new ImmutableVflBlockOperator(this.blockId, this.buffer, startLogId);
        return new BlockResult<>(result.value(), newParent);
    }

    /**
     * Async version - returns CompletableFuture with new instance
     */
    public <T> CompletableFuture<BlockResult<T>> logAsync(
            Function<ImmutableVflBlockOperator, CompletableFuture<T>> fn,
            String preBlockMessage,
            Function<T, String> postBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();

        // Create sub-block
        ImmutableVflBlockOperator block = buffer.createBlock(this.blockId, subBlockId, blockName);

        // Create START log immediately
        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(subBlockId));

        // Create new parent instance immediately (for ordering)
        ImmutableVflBlockOperator newParent = new ImmutableVflBlockOperator(this.blockId, this.buffer, startLogId);

        // Execute async function
        return fn.apply(block)
                .thenApply(result -> {
                    // Create END log when async completes
                    String endLogId = UUID.randomUUID().toString();
                    buffer.createLog(endLogId, this.blockId, startLogId,
                            VflLogType.END, postBlockMessage.apply(result), Set.of(subBlockId));

                    return new BlockResult<>(result, newParent);
                })
                .exceptionally(throwable -> {
                    // Handle errors
                    String endLogId = UUID.randomUUID().toString();
                    buffer.createLog(endLogId, this.blockId, startLogId,
                            VflLogType.END, "Failed: " + throwable.getMessage(), Set.of(subBlockId));

                    throw new RuntimeException(throwable);
                });
    }

    /**
     * Fire-and-forget async - returns new instance immediately
     */
    public ImmutableVflBlockOperator logAsyncFireAndForget(
            Function<ImmutableVflBlockOperator, CompletableFuture<Void>> fn,
            String preBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();

        // Create sub-block
        ImmutableVflBlockOperator block = buffer.createBlock(this.blockId, subBlockId, blockName);

        // Create START log
        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(subBlockId));

        // Start async operation (fire-and-forget)
        fn.apply(block)
                .whenComplete((_, throwable) -> {
                    String endLogId = UUID.randomUUID().toString();
                    String message = throwable != null ?
                            "Failed: " + throwable.getMessage() :
                            "Completed";
                    buffer.createLog(endLogId, this.blockId, startLogId,
                            VflLogType.END, message, Set.of(subBlockId));
                });

        // Return new instance immediately
        return new ImmutableVflBlockOperator(this.blockId, this.buffer, startLogId);
    }

    // Helper record for returning both value and new VFL instance
    public record BlockResult<T>(T value, ImmutableVflBlockOperator vfl) {
    }
}