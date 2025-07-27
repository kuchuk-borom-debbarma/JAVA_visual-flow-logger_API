package threadVfl;

import dev.kuku.vfl.ThreadVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.models.EventPublisherBlock;
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

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.RunEventListener("Sum listener", "Calculating sum of " + a + " " + b, eventPublisherBlock, buffer, () -> {
                var l = ThreadVFL.Get();
                l.log("Starting event listener of sum");
                int r = a + b;
                l.log("Sum = " + r);
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.RunEventListener("Multiply listener", "Multiplying " + a + " and " + b, eventPublisherBlock, buffer, () -> {
                var l = ThreadVFL.Get();
                l.log("Starting event listener of multiply");
                int r = a * b;
                l.log("Multiply = " + r);
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Call("Linear event publisher and listener test", buffer, () -> {
                var logger = ThreadVFL.Get();
                logger.log("Starting event publisher and listener test");
                var publisherBlock = logger.createEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);
                logger.log("Published event now closing");
                return null;
            });
            write("linearEventFlow");
        }
    }
}