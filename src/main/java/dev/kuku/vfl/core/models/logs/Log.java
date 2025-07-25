package dev.kuku.vfl.core.models.logs;

import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Primitive Log that is simple in nature.
 */
@Getter
@RequiredArgsConstructor
public class Log {
    private final String id;
    private final String blockId;
    private final String parentLogId;
    private final LogType logType;
    private final String message;
    private final long timestamp;

    public Log(String id, String blockId, String parentLogId, LogTypeEnum logType, String message, long timestamp) {

        this.id = id;
        this.blockId = blockId;
        this.parentLogId = parentLogId;
        this.logType = new LogType(logType);
        this.message = message;
        this.timestamp = timestamp;
    }
}