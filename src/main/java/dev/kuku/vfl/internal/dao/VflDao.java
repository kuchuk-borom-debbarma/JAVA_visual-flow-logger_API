package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;

import java.util.List;

public interface VflDao {
    /// Upsert to the database. <br>
    /// Conflicting IDs will simply be ignored.<br>
    /// Requires parentBlockID to be valid in database or included in the blocks to upsert.
    void upsertBlocks(List<VflBlockDataType> blocks);
    /// Upsert to database.<br>
    /// Conflicting IDs will be ignored.<br>
    /// Requires blockId to be valid in database or included in the logs to upsert.
    void upsertLogs(List<VflLogDataType> logs);
}
