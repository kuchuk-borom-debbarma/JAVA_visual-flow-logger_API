package dev.kuku.vfl;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class VflBuffer {
    private Map<String, VflBlock> blocks;
    private Map<String, VflLog> logs;

    public VflBuffer(int bufferSize) {
        blocks = new HashMap<>();
        logs = new HashMap<>();
    }

    public VflBlockOperator createBlock(String parentBlockId, String id, String blockName) {
        if (blocks.containsKey(id)) {
            throw new KeyAlreadyExistsException("Block ID " + id + " already exists");
        }
        var block = new VflBlock(parentBlockId, id, blockName);
        blocks.put(id, block);
        return new VflBlockOperator(id, this);
    }
    public VflBlockOperator createBlock(String parentBlockId, String id, String blockName, String latestLogId) {
        if (blocks.containsKey(id)) {
            throw new KeyAlreadyExistsException("Block ID " + id + " already exists");
        }
        var block = new VflBlock(parentBlockId, id, blockName);
        blocks.put(id, block);
        return new VflBlockOperator(id, this, latestLogId);
    }



    public VflLog createLog(String logId, String blockId, String parentLogId, VflLogType logType, String logValue, Set<String> blockPointers) {
        if (logs.containsKey(logId)) {
            throw new KeyAlreadyExistsException("Log ID " + logId + " already exists");
        }
        var log = new VflLog(logId, blockId, parentLogId, logType, logValue, blockPointers, Instant.now().toEpochMilli());
        logs.put(logId, log);
        return log;
    }
}
