package dev.kuku.vfl.core.models;

public class LogData {
    private String id;
    private String blockId;
    private String parentLogId;
    private VflLogType logType;
    private String message;
    private String referenceValue;
    private long timestamp;

    public LogData() {
    }

    public LogData(String id, String blockId, String parentLogId, VflLogType logType, String message, String referenceValue, long timestamp) {
        this.id = id;
        this.blockId = blockId;
        this.parentLogId = parentLogId;
        this.logType = logType;
        this.message = message;
        this.referenceValue = referenceValue;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getParentLogId() {
        return parentLogId;
    }

    public void setParentLogId(String parentLogId) {
        this.parentLogId = parentLogId;
    }

    public VflLogType getLogType() {
        return logType;
    }

    public void setLogType(VflLogType logType) {
        this.logType = logType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(String referenceValue) {
        this.referenceValue = referenceValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

