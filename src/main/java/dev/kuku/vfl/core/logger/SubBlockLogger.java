package dev.kuku.vfl.core.logger;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;

/**
 * A Block logger child class that can't be closed automatically and thus needs to be closed manually by calling close methoc.
 */
public class SubBlockLogger extends BlockLogger {
    public SubBlockLogger(BlockData parentBlockData, BlockData blockData, VFLBuffer buffer) {
        super(blockData, buffer);
    }

    public void closeSubBlock() {
        //TODO
    }
}
