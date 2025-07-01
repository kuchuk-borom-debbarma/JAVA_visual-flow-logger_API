package dev.kuku.vfl.models;

import java.util.Optional;
import java.util.Set;

public class LogData {
    private final String id;
    private final String blockId;
    private final String parentLogId;
    private final VflLogType logType;
    private final String logValue;
    private final Set<String> blockPointers;
    private final long timeStamp;

    public LogData(String id, String blockId, String parentLogId, VflLogType logType, String logValue, Set<String> blockPointers, long timeStamp) {
        if (id == null || blockId == null) {
            throw new IllegalArgumentException("id, blockId can not be null");
        }
        this.id = id;
        this.blockId = blockId;
        this.parentLogId = parentLogId;
        this.logType = logType;
        this.logValue = logValue;
        this.blockPointers = blockPointers;
        this.timeStamp = timeStamp;
    }

    public String getId() {
        return id;
    }

    public String getBlockId() {
        return blockId;
    }

    public Optional<String> getParentLogId() {
        return Optional.ofNullable(parentLogId);
    }

    public VflLogType getLogType() {
        return logType;
    }

    public Optional<String> getLogValue() {
        return Optional.ofNullable(logValue);
    }

    public Optional<Set<String>> getBlockPointers() {
        return Optional.ofNullable(blockPointers);
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
