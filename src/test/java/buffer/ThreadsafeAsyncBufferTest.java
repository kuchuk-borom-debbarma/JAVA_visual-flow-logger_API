package buffer;

import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable;
import dev.kuku.vfl.variants.thread_local.FluentThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadsafeAsyncBufferTest {
    ThreadSafeInMemoryFlushHandlerImpl flush = new ThreadSafeInMemoryFlushHandlerImpl();
    VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(1, 5000, flush, Executors.newFixedThreadPool(10));
    VFLBuffer b = new ThreadSafeSynchronousVflBuffer(1, flush);
    FluentVFLCallable f;

    void write(String fileName) {
        try {
            String path = "test/output/vflBuffer/threadSafeAsync/stressTest";
            Files.createDirectories(Path.of(path));
            try (FileWriter f = new FileWriter(path + "/" + fileName + ".json")) {
                f.write(flush.generateNestedJsonStructure());
                flush.cleanup();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class StressTestPush {
        @Test
        void multiThread() {
            ThreadVFL.Runner.Instance.StartVFL("Single Thread", b, () -> {
                FluentThreadVFL.Log("Starting single thread buffer test");
                int size = 10000;
                List<CompletableFuture<Void>> sums = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int finalI = i;
                    var t = f.runSubBlock(() -> sum(finalI))
                            .withBlockName("Sum Block no. " + i)
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
            write("multiThread");
        }

        void sum(int i) {
            f.log("Summing " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            f.log("Sum is " + i + i);
        }
    }
}
