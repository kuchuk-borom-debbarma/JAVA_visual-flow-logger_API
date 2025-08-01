package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class AsyncVFLBuffer extends VFLBufferBase {
    private final ExecutorService flushExecutor;
    private final ScheduledExecutorService periodicExecutor;
    private final VFLFlushHandler flushHandler;
    private final int flushTimeout;

    public AsyncVFLBuffer(int bufferSize, int finalFlushTimeoutMillisecond, int periodicFlushTimeMillisecond, VFLFlushHandler flushHandler, ExecutorService bufferFlushExecutor, ScheduledExecutorService periodicFlushExecutor) {
        super(bufferSize);
        this.flushExecutor = bufferFlushExecutor;
        this.periodicExecutor = periodicFlushExecutor;
        this.flushHandler = flushHandler;
        this.flushTimeout = finalFlushTimeoutMillisecond;
        periodicExecutor.scheduleWithFixedDelay(this::flushAll, periodicFlushTimeMillisecond, periodicFlushTimeMillisecond, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, Pair<Long, String>> blockEnds) {
        flushExecutor.submit(() -> {
            flushHandler.pushBlocksToServer(blocks);
            flushHandler.pushBlockStartsToServer(blockStarts);
            flushHandler.pushBlockEndsToServer(blockEnds);
            flushHandler.pushLogsToServer(logs);
        });
    }

    @Override
    public void flushAndClose() {
        super.flushAndClose();
        periodicExecutor.shutdown();
        flushExecutor.shutdown();
        try {
            //Wait for 100 ms and recheck until we reach flushTimeout
            int current = 0;
            while (current < flushTimeout) {
                current += 100;
                if (flushExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    flushHandler.closeFlushHandler();
                    return;
                }
            }
            throw new TimeoutException("Waiting time for flushing exceeded configured flush timeout " + flushTimeout);
        } catch (Exception e) {
            log.error("Failed to close flush and close {}:{}", e.fillInStackTrace(), e.getMessage());
        }
    }
}
