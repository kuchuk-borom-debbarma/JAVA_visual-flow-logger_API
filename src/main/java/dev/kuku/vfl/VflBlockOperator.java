package dev.kuku.vfl;


import java.util.HashMap;
import java.util.Map;
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

    /**
     * @param blockName name of the block
     * @param message   message to log after the block operation is complete
     * @param fn        function to run in the block
     */
    public <T> T startNestedBlock(String blockName, Function<T, String> message, Function<VflBlockOperator, T> fn) {
        String blockId = UUID.randomUUID().toString();
        String logId = UUID.randomUUID().toString();
        var block = buffer.createBlock(this.blockId, blockId, blockName);
        buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.SUB_BLOCK_START, null, new String[]{blockId});
        T result = fn.apply(block);
        buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.SUB_BLOCK_END, message.apply(result), null);
        return result;
    }

    public <T> Map<String, T> branch(Map<String, Function<VflBlockOperator, T>> branches) {
        //TODO FIX. Create log of type branch and then create blockIds for them mapped to the name. Then create block for each passed function
        Map<String, T> results = new HashMap<>();
        branches.forEach((name, branch) -> {
            String blockId = UUID.randomUUID().toString();
            var block = buffer.createBlock(this.blockId, blockId, name, this.latestLogId);
            results.put(name, branch.apply(block));
        };
        return results;
    }
}
