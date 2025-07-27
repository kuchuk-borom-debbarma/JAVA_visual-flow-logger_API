package dev.kuku.vfl.core.models.logs.enums;

public enum LogTypeEnum {
    MESSAGE("MESSAGE"),
    WARN("WARN"),
    ERROR("ERROR");

    private final String DisplayName;

    LogTypeEnum(String displayName) {
        DisplayName = displayName;
    }
}
