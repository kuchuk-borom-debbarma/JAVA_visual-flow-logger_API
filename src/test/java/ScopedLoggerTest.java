import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVirtualThreadVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.io.IOException;


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
        void run() {
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

    }

}
