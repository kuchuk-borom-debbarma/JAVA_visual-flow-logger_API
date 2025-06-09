package dev.kuku.vfl;

import java.util.Optional;

 class VflLog {
    private final String id;
    private final String blockId;
    private final String parentLogId;
    private final VflLogType logType;
    private final String logValue;
    private final String[] blockPointers;
    private final long timeStamp;

    public VflLog(String id, String blockId, String parentLogId, VflLogType logType, String logValue, String[] blockPointers, long timeStamp) {
        if (id == null || blockId == null) {
            throw new IllegalArgumentException("id, blockId can not be null");
        }
        if (blockPointers == null) {
            blockPointers = new String[0];
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

    public String[] getBlockPointers() {
        return blockPointers;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
