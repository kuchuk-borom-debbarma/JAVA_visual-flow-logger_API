package dev.kuku.dto;

import dev.kuku.vfl.models.VflLogType;

public class LogDTO {
    public String id;
    public String parentLogId;
    public String blockId;
    public String message;
    public VflLogType logType;
    public String referenceValue;
    public long timestamp;

    //Required for JSON
    public LogDTO(String id, String parentLogId, String blockId, String message, VflLogType logType, String referenceValue, long timestamp) {
        this.id = id;
        this.parentLogId = parentLogId;
        this.blockId = blockId;
        this.message = message;
        this.logType = logType;
        this.referenceValue = referenceValue;
        this.timestamp = timestamp;
    }

    public LogDTO() {
    }

}
