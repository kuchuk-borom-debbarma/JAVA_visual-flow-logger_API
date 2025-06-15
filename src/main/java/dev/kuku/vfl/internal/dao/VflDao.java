package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;

import java.util.List;

public interface VflDao {
    /**
     * Upsert blocks to datastore.<br>
     * On ID conflicting, skip updating it.<br>
     * For a block with parentBlockId, the ID must be of an existing block OR ID of a block that is being inserted.
     * @param blocks blocks to insert
     */
    void upsertBlocks(List<VflBlockDataType> blocks);
    /// Upsert to database.<br>
    /// Conflicting IDs will be ignored.<br>
    /// Requires blockId to be valid in database or included in the logs to upsert.
    void upsertLogs(List<VflLogDataType> logs);
}
