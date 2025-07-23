package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadSafeAsyncVirtualThreadVFLBuffer implements VFLBuffer {
    private final int blockSize;
    private final int logSize;
    private final List<Log> logsToFlush;
    private final List<Block> blocksToFlush;
    private final VFLFlushHandler flushHandler;
    private final ExecutorService executor;
    private final Object lock = new Object();

    public ThreadSafeAsyncVirtualThreadVFLBuffer(int blockSize, int logSize, VFLFlushHandler flushHandler) {
        this.blockSize = blockSize;
        this.logSize = logSize;
        this.logsToFlush = new ArrayList<>();
        this.blocksToFlush = new ArrayList<>();
        this.flushHandler = flushHandler;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void pushLogToBuffer(Log log) {
        synchronized (lock) {
            logsToFlush.add(log);
        }
        //Ok to not be locked. If data changes in between it should be good since we lock within flush
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        synchronized (lock) {
            blocksToFlush.add(block);
        }
        //Ok to not be locked. If data changes in between it should be good since we lock within flush
        flushIfFull();
    }

    private void flushIfFull() {
        boolean shouldFlush = false;
        synchronized (lock) {
            if (blocksToFlush.size() >= blockSize || logsToFlush.size() >= logSize) {
                shouldFlush = true;
            }
        }
        if (shouldFlush) {
            //Not locked but we do lock inside so this is again fine
            flush();
        }
    }

    private void flush() {
        List<Log> l;
        List<Block> b;
        synchronized (lock) {
            l = new ArrayList<>(logsToFlush);
            logsToFlush.clear();
            b = new ArrayList<>(blocksToFlush);
            blocksToFlush.clear();
        }
        executor.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!b.isEmpty())
                flushHandler.pushBlocksToServer(b);
            if (!l.isEmpty())
                flushHandler.pushLogsToServer(l);
        });
    }

    private void flushBlocking() {
        List<Log> l;
        List<Block> b;
        synchronized (lock) {
            l = new ArrayList<>(logsToFlush);
            logsToFlush.clear();
            b = new ArrayList<>(blocksToFlush);
            blocksToFlush.clear();
        }

        // Execute synchronously in the current thread
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!b.isEmpty())
            flushHandler.pushBlocksToServer(b);
        if (!l.isEmpty())
            flushHandler.pushLogsToServer(l);
    }

    @Override
    public void flushAndClose() {
        // Flush all remaining data synchronously
        flushBlocking();
    }

}