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

/**
 * Asynchronous buffer implementation of {@link VFLBuffer} that batches log and block data
 * and flushes it asynchronously using an {@link ExecutorService}.
 *
 * <p>This buffer also supports periodic flushing at a fixed interval using a {@link ScheduledExecutorService}.
 * Data is flushed in batches when the buffer is full or at the periodic interval, whichever comes first.
 *
 * <p><b>Main features:</b>
 * <ul>
 *   <li>Batches incoming logs, blocks, and start/end events for efficient flushing</li>
 *   <li>Flushes buffered data asynchronously via a provided {@link ExecutorService}</li>
 *   <li>Periodically flushes at a configurable interval via a scheduled executor</li>
 *   <li>Gracefully shuts down executors and flushes all pending data on {@link #flush()}</li>
 *   <li>Falls back to synchronous flush if the executor rejects tasks (e.g., during shutdown)</li>
 * </ul>
 *
 * <p><b>Usage notes:</b>
 * <ul>
 *   <li>The {@code flushTimeout} controls how long the buffer waits for async flush tasks
 *       to complete during {@link #flush()} shutdown before forcing a shutdown</li>
 *   <li>Construct with your own configured executors and flush handler implementation</li>
 *   <li>Make sure to call {@link #flush()} at shutdown to avoid losing buffered data</li>
 * </ul>
 */
@Slf4j
public class AsyncBuffer extends VFLBufferWithFlushHandlerBase {

    private final ExecutorService flushExecutor;
    private final ScheduledExecutorService periodicExecutor;
    private final int flushTimeout;

    /**
     * Constructs an AsyncBuffer instance.
     *
     * @param bufferSize               max number of buffered items before automatic flush
     * @param finalFlushTimeoutMillisecond max millis to wait for async flush tasks to complete on shutdown
     * @param periodicFlushTimeMillisecond interval in millis to trigger periodic flushes
     * @param flushHandler             handler responsible for sending flushed data to destination
     * @param bufferFlushExecutor      executor for async flush task execution
     * @param periodicFlushExecutor    scheduled executor for periodic flush triggers
     */
    public AsyncBuffer(int bufferSize,
                       int finalFlushTimeoutMillisecond,
                       int periodicFlushTimeMillisecond,
                       VFLFlushHandler flushHandler,
                       ExecutorService bufferFlushExecutor,
                       ScheduledExecutorService periodicFlushExecutor) {
        super(bufferSize, flushHandler);
        this.flushExecutor = bufferFlushExecutor;
        this.periodicExecutor = periodicFlushExecutor;
        this.flushTimeout = finalFlushTimeoutMillisecond;

        // Schedule periodic flushes at fixed delay
        periodicExecutor.scheduleWithFixedDelay(this::flushAll,
                periodicFlushTimeMillisecond,
                periodicFlushTimeMillisecond,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Executes the flush asynchronously using the flush executor.
     * If the executor is shut down or rejects the task, performs a synchronous flush.
     */
    @Override
    protected void executeFlushAll(List<Log> logs,
                                   List<Block> blocks,
                                   Map<String, Long> blockStarts,
                                   Map<String, BlockEndData> blockEnds) {
        if (flushExecutor.isShutdown()) {
            log.debug("Executor is shutdown, performing synchronous flush");
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

    /**
     * Flushes all remaining data, shuts down executors with a timeout,
     * and closes the flush handler.
     *
     * @throws RuntimeException if the flush executor did not terminate within the timeout
     */
    @Override
    public void flush() {
        // Stop periodic flushes to avoid race conditions during shutdown
        periodicExecutor.shutdown();

        // Flush any buffered data
        super.flushAll();

        // Shutdown flush executor and wait for tasks to complete
        flushExecutor.shutdown();

        try {
            int elapsed = 0;
            while (elapsed < flushTimeout) {
                if (flushExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    log.debug("Finished flush executor shutdown. Closing flush handler.");
                    flushHandler.closeFlushHandler();
                    log.debug("Finished complete flush");
                    return;
                }
                elapsed += 100;
            }

            // Timeout reached - force shutdown and report error
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
