package dev.kuku.vfl;


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

    public void log(String message) {
        String logId = UUID.randomUUID().toString();
        var log = buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.MESSAGE, message, null);
        this.latestLogId = log.getId();
    }

    public <T> T startNestedBlock(String blockName, Function<VflBlockOperator, T> fn) {
        String id = UUID.randomUUID().toString();
        var block = buffer.createBlock(this.blockId, id, blockName);
        return fn.apply(block);

    }
}
