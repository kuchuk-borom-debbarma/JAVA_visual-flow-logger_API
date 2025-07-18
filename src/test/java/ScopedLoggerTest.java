import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVirtualThreadVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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
            logger.text("Sum " + a + " and " + b);
            return logger.textFn(() -> a + b, integer -> "Sum is " + integer);
        }

        int multiply(int a, int b) {
            logger.text("Multiply " + a + " and " + b);
            return logger.textFn(() -> a * b, integer -> "Multiply is " + integer);
        }

        @Test
        void nonLinearFlow() throws InterruptedException, Exception {
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

    }

}
