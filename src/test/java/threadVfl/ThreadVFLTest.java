package threadVfl;

import dev.kuku.vfl.ThreadVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThreadVFLTest {

    private final InMemoryFlushHandlerImpl flush = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(999999, 99999, flush);

    void write(String fileName) {
        try {
            String path = "test/output/threadVFL/logger";
            Files.createDirectories(Path.of(path));
            try (FileWriter f = new FileWriter(path + "/" + fileName + ".json")) {
                f.write(flush.toNestedJson());
                flush.cleanup();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Call("Simple Linear test", buffer, () -> {
                var logger = ThreadVFL.get();
                logger.log("This is a log #1");
                logger.log("Now going to start another block");
                //TODO fix
                int result = logger.callPrimarySubBlock("Sum block", "Doing sum of 1, 2", () -> sum(1, 2), integer -> "Calculated sum is " + integer);
                logger.log("So now the result is " + result);
                return null;
            });
            write("linearFlow");
        }

        int sum(int a, int b) {
            var logger = ThreadVFL.get();
            logger.log("Going to sum " + a + " " + b);
            logger.log("Sum = " + a + b);
            return a + b;
        }
    }
}