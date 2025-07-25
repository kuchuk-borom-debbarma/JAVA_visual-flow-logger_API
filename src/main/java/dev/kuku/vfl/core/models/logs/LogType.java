package dev.kuku.vfl.core.models.logs;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

public class LogType {
    public final String value;

    public LogType(LogTypeEnum logType) {
        this.value = logType.toString();
    }

    public LogType(LogTypeBlockStartEnum logType) {
        this.value = logType.toString();
    }
}