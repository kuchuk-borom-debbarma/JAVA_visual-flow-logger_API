package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;

public class ScopedBlockLoggerStarter {
    public static BlockLog start(String blockName, VFLBuffer buffer) {
        return new ScopedBlockLogImpl(buffer, new BlockData(UUID.randomUUID().toString(), null, blockName));
    }
}
