package dev.kuku.vfl.core.models.logs;

import lombok.Getter;

import java.time.Instant;

/**
 * Sub block start that is not part of the main flow. It may be a fire and forget type of block which doesn't join back to main flow based on currentLogId after operation is complete
 * <p>
 * Joining will be a separate from creating block push similar to block start and block end
 * TODO Make block start, block end, block join its own thing instead of treating as block.
 */
@Getter
public class SubBlockStartSecondary extends Log {
    private final boolean joins;

    public SubBlockStartSecondary(String id, String blockId, String parentLogId, String logType, String message, boolean joins) {
        super(id, blockId, parentLogId, "SUB_BLOCK_START_SECONDARY", message, Instant.now().toEpochMilli());
        this.joins = joins;
    }
}