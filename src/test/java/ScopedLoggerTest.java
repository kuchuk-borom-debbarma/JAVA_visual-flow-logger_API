import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVirtualThreadVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


public class ScopedLoggerTest {
    private static ScopedVFL logger;

    @DisplayName("Simple linear test")
    @Nested
    class SimpleTest {
        InMemoryFlushHandlerImpl flushHandler;
        VFLBuffer buffer;

        @BeforeEach
        void beforeEach() {
            flushHandler = new InMemoryFlushHandlerImpl();
            buffer = new ThreadSafeAsyncVirtualThreadVFLBuffer(100, 100, flushHandler);
        }

        @AfterEach
        void afterEach() throws IOException {
            try (var writer = new FileWriter("nested.json")) {
                writer.write(flushHandler.toJsonNested());
            }
        }

        @Test
        void linearFlow() {
            ScopedVFLRunner.run("Simple linear test", buffer, () -> {
                logger = ScopedVFLImpl.get();
                logger.text("Starting simple linear test");
                //Logging return value + sub block started at the same code
                int sum =
                        logger.textFn(
                                () -> logger.call("Sum(1,2)", "Calculating sum..", () -> sum(1, 2)),
                                integer -> "Calculated sum is " + integer
                        );
            });
        }

        int sum(int a, int b) {
            logger.text("Summing in thread ID : " + Thread.currentThread().threadId());
            logger.text("Sum " + a + " and " + b);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return logger.textFn(() -> a + b, integer -> "Sum is " + integer);
        }

        int multiply(int a, int b) {
            logger.text("Multiply " + a + " and " + b);
            return logger.textFn(() -> a * b, integer -> "Multiply is " + integer);
        }

        @Test
        void nonLinearFlow() {
            ScopedVFLRunner.run("Simple non-linear test", buffer, () -> {
                logger = ScopedVFLImpl.get();
                logger.text("Starting simple non-linear test");
                var sumTask = (CompletableFuture<Integer>) logger.callHereAsync("Sum(1,2)", "Sum1 called", () -> sum(1, 2));
                var multiplyTask = (CompletableFuture<Integer>) logger.callHereAsync("multiply(2,2)", "Multiply called..", () -> multiply(2, 2));
                var futures = CompletableFuture.allOf(sumTask, multiplyTask);

                try {
                    futures.get(); //execute both futures async way
                    //Since both complete we can fetch result
                    logger.text("Calculated sum is " + sumTask.get());
                    logger.text("Calculated product is " + multiplyTask.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Test
        void heavyAsync() {
            ScopedVFLRunner.run("Heavy Async test", buffer, () -> {
                logger = ScopedVFLImpl.get();
                logger.text("Starting simple heavy async test");
                int count = 10000;
                List<CompletableFuture<Integer>> futures = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int finalI = i;
                    futures.add((CompletableFuture<Integer>) logger.callHereAsync("" + finalI, null, () -> sum(finalI, finalI), Executors.newVirtualThreadPerTaskExecutor()));
                }

                var collected = futures.stream()
                        .map(integerCompletableFuture -> {
                            try {
                                return integerCompletableFuture.get();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();
                logger.text("Calculated product is " + collected);
            });
        }

    }

}
