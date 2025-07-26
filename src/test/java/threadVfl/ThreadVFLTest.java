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
import java.util.concurrent.CompletableFuture;

public class ThreadVFLTest {

    private final InMemoryFlushHandlerImpl flush = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(999999, 99999, flush);

    void write(String fileName) {
        try {
            String path = "test/output/threadVFL/logger";
            Files.createDirectories(Path.of(path));
            try (FileWriter f = new FileWriter(path + "/" + fileName + ".json")) {
                f.write(flush.generateNestedJsonStructure());
                flush.cleanup();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    int sum(int a, int b) {
        var logger = ThreadVFL.Get();
        logger.log("Going to sum " + a + " " + b);
        logger.log("Sum = " + a + b);
        return a + b;
    }

    int multiply(int a, int b) {
        var logger = ThreadVFL.Get();
        logger.log("Multiplying " + a + " and " + b);
        int result = a * b;
        logger.log("Multiplying value = " + result);
        return result;
    }


    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Call("Simple Linear test", buffer, () -> {
                var logger = ThreadVFL.Get();
                logger.log("This is a log #1");
                logger.log("Now going to start another block");
                int result = logger.callPrimarySubBlock("Sum block", "Doing sum of 1, 2", () -> sum(1, 2), integer -> "Calculated sum is " + integer);
                logger.log("So now the result is " + result);
                return null;
            });
            write("linearFlow");
        }
    }

    @Nested
    class AsyncFlow {
        @Test
        void asyncTest() {
            ThreadVFL.Runner.Call("AsyncFlow Test", buffer, () -> {
                var l = ThreadVFL.Get();
                l.log("Starting async test now...");
                int r = l.callPrimarySubBlock("Sum primary", "Starting primary sum block first", () -> sum(1, 2), integer -> "Result of sum block is " + integer);
                CompletableFuture<Integer> t1 = l.callSecondaryJoiningBlock("Sum async", "Squaring in async", () -> {
                    var m = ThreadVFL.Get();
                    m.log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    m.log("Finished sleeping");
                    return sum(1, 2);
                }, integer -> "Result is " + integer, null);
                CompletableFuture<Integer> t2 = l.callSecondaryJoiningBlock("Multiply async", "Multiply in async", () -> {
                    var m = ThreadVFL.Get();
                    m.log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    m.log("Finished sleeping");
                    return multiply(1, 2);
                }, integer -> "Result is " + integer, null);

                var a = t1.get();
                var b = t2.get();

                l.callPrimarySubBlock("Sum primary 2", "Doing sum of results", () -> sum(a, b), integer -> "Final result = " + integer);
                l.log("Everything is DONE and dusted!!!");
                return null;
            });
            write("AsyncFlow");
        }
    }
}