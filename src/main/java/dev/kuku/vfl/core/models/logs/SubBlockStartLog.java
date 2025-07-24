package dev.kuku.vfl.core.models.logs;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;
import lombok.Getter;

import java.time.Instant;

/**
 * Sub block start log following block's flow chain.
 */
@Getter
public class SubBlockStartLog extends Log {
    //The block that is being started
    private final String referencedBlockId;

    public SubBlockStartLog(String id, String blockId, String parentLogId, String startMessage, String referencedBlockId, LogTypeBlcokStartEnum logType) {
        super(id, blockId, parentLogId, new LogType(logType), startMessage, Instant.now().toEpochMilli());
        this.referencedBlockId = referencedBlockId;
    }

    public SubBlockStartLog(Log log, String referencedBlockId, LogTypeBlcokStartEnum logTypeBlcokStartEnum) {
        super(log.getId(), log.getBlockId(), log.getParentLogId(), new LogType(logTypeBlcokStartEnum), log.getMessage(), Instant.now().toEpochMilli());
        this.referencedBlockId = referencedBlockId;
    }
}