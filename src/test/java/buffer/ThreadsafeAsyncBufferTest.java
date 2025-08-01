package buffer;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.SynchronousVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.DummyFlushHandler;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.variants.thread_local.FluentThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadsafeAsyncBufferTest {
    private final NestedJsonFlushHandler flush = new NestedJsonFlushHandler("test/output/buffer_test.json");
    private final DummyFlushHandler dummy = new DummyFlushHandler();
    private final VFLBuffer buffer = new AsyncVFLBuffer(100, 5000, 5000, flush, Executors.newFixedThreadPool(160), Executors.newScheduledThreadPool(2));
    private final VFLBuffer b = new SynchronousVFLBuffer(100, flush);


    @Nested
    class StressTestPush {
        @Test
        void multiThread() {
            ThreadVFL.Runner.Instance.StartVFL("Single Thread", buffer, () -> {
                FluentThreadVFL.Log("Starting single thread buffer test");
                int size = 10000;
                List<CompletableFuture<Void>> sums = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int finalI = i;
                    var t = FluentThreadVFL.RunSubBlock(() -> sum(finalI))
                            .asBlock("Sum Block no. " + i)
                            .withStartMessage("Summing " + i)
                            .startSecondaryJoining(Executors.newVirtualThreadPerTaskExecutor());
                    sums.add(t);
                }
                sums.forEach(voidCompletableFuture -> {
                    try {
                        voidCompletableFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                return null;
            });
        }

        void sum(int i) {
            FluentThreadVFL.Log("Summing " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            FluentThreadVFL.Log("Sum is " + i + i);
        }
    }
}
