package dev.kuku.vfl.serviceCall;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;

import java.util.List;

public interface VFLApi {
    boolean pushLogsToServer(List<LogData> logs);

    boolean pushBlocksToServer(List<BlockData> blocks);
}
