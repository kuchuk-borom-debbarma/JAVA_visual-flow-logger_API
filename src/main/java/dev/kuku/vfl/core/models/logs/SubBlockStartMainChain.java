package dev.kuku.vfl.core.models.logs;

import lombok.Getter;

import java.time.Instant;

/**
 * Sub block start log following block's flow chain.
 */
@Getter
public class SubBlockStartMainChain extends Log {
    //The block that is being started
    private final String referencedBlockId;

    public SubBlockStartMainChain(String id, String blockId, String parentLogId, String startMessage, String referencedBlockId, boolean isPartOfMainFlow) {
        super(id, blockId, parentLogId, "SUB_BLOCK_START_MAIN", startMessage, Instant.now().toEpochMilli());
        this.referencedBlockId = referencedBlockId;
    }
}