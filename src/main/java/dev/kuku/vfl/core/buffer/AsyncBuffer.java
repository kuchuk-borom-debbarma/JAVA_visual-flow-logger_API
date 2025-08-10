package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.abstracts.VFLBufferWithFlushHandlerBase;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AsyncBuffer extends VFLBufferWithFlushHandlerBase {
    private final ExecutorService flushExecutor;
    private final ScheduledExecutorService periodicExecutor;
    private final int flushTimeout;

    public AsyncBuffer(int bufferSize, int finalFlushTimeoutMillisecond, int periodicFlushTimeMillisecond,
                       VFLFlushHandler flushHandler, ExecutorService bufferFlushExecutor,
                       ScheduledExecutorService periodicFlushExecutor) {
        super(bufferSize, flushHandler);
        this.flushExecutor = bufferFlushExecutor;
        this.periodicExecutor = periodicFlushExecutor;
        this.flushTimeout = finalFlushTimeoutMillisecond;
        periodicExecutor.scheduleWithFixedDelay(this::flushAll, periodicFlushTimeMillisecond, periodicFlushTimeMillisecond, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void executeFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, BlockEndData> blockEnds) {
        // Guard against shutdown executor
        if (flushExecutor.isShutdown()) {
            log.warn("Executor is shutdown, performing synchronous flush");
            performOrderedFlush(logs, blocks, blockStarts, blockEnds);
            return;
        }

        try {
            flushExecutor.submit(() -> performOrderedFlush(logs, blocks, blockStarts, blockEnds));
        } catch (RejectedExecutionException e) {
            log.warn("Task rejected by executor (likely shutting down), performing synchronous flush", e);
            performOrderedFlush(logs, blocks, blockStarts, blockEnds);
        }
    }

    @Override
    public void flush() {
        // First, stop the periodic executor to prevent new scheduled flushes
        periodicExecutor.shutdown();
        super.flushAll();
        // Now shutdown the flush executor
        flushExecutor.shutdown();

        try {
            int elapsed = 0;
            while (elapsed < flushTimeout) {
                if (flushExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    // Tasks completed successfully
                    log.debug("Finished flush executor shutdown. Closing flush handler.");
                    flushHandler.closeFlushHandler();
                    log.debug("Finished Complete flush");
                    return;
                }
                elapsed += 100;
            }

            // Timeout exceeded - force shutdown
            flushExecutor.shutdownNow();
            throw new RuntimeException("Flush timeout exceeded: " + flushTimeout + "ms");

        } catch (InterruptedException e) {
            flushExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during shutdown", e);
        }
    }

    @Override
    public String toString() {
        return "AsyncBuffer{" +
                "flushTimeout=" + flushTimeout +
                ", flushHandler=" + flushHandler +
                ", periodicExecutor=" + periodicExecutor +
                ", flushExecutor=" + flushExecutor +
                '}';
    }
}