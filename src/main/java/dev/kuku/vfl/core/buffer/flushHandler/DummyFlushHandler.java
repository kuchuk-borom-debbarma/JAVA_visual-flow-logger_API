package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

public class DummyFlushHandler implements VFLFlushHandler {
    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        return true;
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        return true;
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, Pair<Long, String>> blockEnds) {
        return true;
    }

    @Override
    public void closeFlushHandler() {

    }
}
