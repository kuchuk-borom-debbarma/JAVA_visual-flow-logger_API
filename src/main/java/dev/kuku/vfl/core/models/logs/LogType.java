package dev.kuku.vfl.core.models.logs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    // Private constructor for Jackson deserialization
    private LogType(String value) {
        this.value = value;
    }

    @JsonValue  // This tells Jackson to serialize this object as just the string value
    @Override
    public String toString() {
        return this.value;
    }

    @JsonCreator  // This tells Jackson how to create the object from a string
    public static LogType fromString(String value) {
        return new LogType(value);
    }
}