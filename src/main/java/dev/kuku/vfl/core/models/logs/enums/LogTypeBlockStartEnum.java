package dev.kuku.vfl.core.models.logs.enums;

public enum LogTypeBlockStartEnum {
    SUB_BLOCK_START_PRIMARY("SUB_BLOCK_START_PRIMARY"),
    SUB_BLOCK_START_SECONDARY_NO_JOIN("SUB_BLOCK_START_SECONDARY_NO_JOIN"),
    PUBLISH_EVENT("PUBLISH_EVENT"),
    SUB_BLOCK_START_SECONDARY_JOIN("SUB_BLOCK_START_SECONDARY_JOIN"),
    SUB_BLOCK_CONTINUE("SUB_BLOCK_CONTINUE"),
    SUB_BLOCK_CONTINUE_COMPLETE("SUB_BLOCK_CONTINUE_COMPLETE"),
    EVENT_LISTENER("EVENT_LISTENER");

    private final String displayName;

    // Constructor to set the display name
    LogTypeBlockStartEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}