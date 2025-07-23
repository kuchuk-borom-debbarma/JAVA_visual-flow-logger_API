package dev.kuku.vfl.core.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LogData {
    private final String id;
    private final String blockId;
    private final String parentLogId;
    private final String logType;
    private final String message;
    private final long timestamp;
}

