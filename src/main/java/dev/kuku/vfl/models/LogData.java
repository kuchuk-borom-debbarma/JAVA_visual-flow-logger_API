package dev.kuku.vfl.models;

public class LogData {
    private String id;
    private String blockId;
    private String parentLogId;
    private VflLogType logType;
    private String message;
    private String referenceValue;
    private long timeStamp;

    public LogData() {
    }

    public LogData(String id, String blockId, String parentLogId, VflLogType logType, String message, String referenceValue, long timeStamp) {
        this.id = id;
        this.blockId = blockId;
        this.parentLogId = parentLogId;
        this.logType = logType;
        this.message = message;
        this.referenceValue = referenceValue;
        this.timeStamp = timeStamp;
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

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}

