package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

/**
 * A {@link VFLBuffer} implementation that discards all data.
 *
 * <p>This is a "no‑operation" (no‑op) buffer — all calls to push logs,
 * blocks, or block lifecycle events are ignored, and {@link #flush()} does nothing.</p>
 *
 * <h2>Use cases:</h2>
 * <ul>
 *   <li><b>Disable VFL output</b> completely while keeping the code paths intact</li>
 *   <li>Run unit tests or benchmarks without generating log output</li>
 *   <li>Use as a safe fallback when no actual buffer implementation is configured</li>
 * </ul>
 *
 * <h2>Important:</h2>
 * <ul>
 *   <li>All trace/log data is discarded immediately — nothing is written to memory, files, or downstream systems</li>
 *   <li>Intended for scenarios where you explicitly do <b>not</b> need VFL logs or metrics</li>
 * </ul>
 */
public class NoOpsBuffer implements VFLBuffer {

    @Override
    public void pushLogToBuffer(Log log) {
        // Do nothing
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        // Do nothing
    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        // Do nothing
    }

    @Override
    public void pushLogEndToBuffer(String blockId, BlockEndData endData) {
        // Do nothing
    }

    @Override
    public void flush() {
        // Do nothing
    }
}
