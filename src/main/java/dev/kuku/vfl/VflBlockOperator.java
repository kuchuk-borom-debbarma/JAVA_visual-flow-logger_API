package dev.kuku.vfl;


import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class VflBlockOperator {
    private final String blockId;
    private final VflBuffer buffer;
    private String latestLogId;

    public VflBlockOperator(String blockId, VflBuffer buffer) {
        this.blockId = blockId;
        this.buffer = buffer;
    }

    public VflBlockOperator(String blockId, VflBuffer buffer, String latestLogId) {
        this(blockId, buffer);
        this.latestLogId = latestLogId;
    }

    public void log(String message) {
        String logId = UUID.randomUUID().toString();
        var log = buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.MESSAGE, message, null);
        this.latestLogId = log.getId();
    }

    public <T> T log(Function<VflBlockOperator, T> fn, String preBlockMessage, Function<T, String> postBlockMessage, String blockName) {
        String blockId = UUID.randomUUID().toString();
        String logId = UUID.randomUUID().toString();
        var block = buffer.createBlock(this.blockId, blockId, blockName);
        buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(blockId));
        T result = fn.apply(block);
        buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.SUB_BLOCK_END, postBlockMessage.apply(result), Set.of(blockId));
        return result;
    }
}
