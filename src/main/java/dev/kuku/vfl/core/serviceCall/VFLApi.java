package dev.kuku.vfl.core.serviceCall;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.List;

public interface VFLApi {
    boolean pushLogsToServer(List<LogData> logs);

    boolean pushBlocksToServer(List<BlockData> blocks);
}
