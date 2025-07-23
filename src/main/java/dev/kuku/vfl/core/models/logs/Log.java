package dev.kuku.vfl.core.models.logs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Primitive Log that is simple in nature.
 */
@RequiredArgsConstructor
@Getter
public class Log {
    private final String id;
    private final String blockId;
    private final String parentLogId;
    //MESSAGE, WARN, ERROR, SUB_BLOCK_START
    private final String logType;
    private final String message;
    private final long timestamp;
}