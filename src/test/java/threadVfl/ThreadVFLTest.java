package threadVfl;

import dev.kuku.vfl.ThreadVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadVFLTest {

    private final ThreadSafeInMemoryFlushHandlerImpl flush = new ThreadSafeInMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(999999, flush);

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
        ThreadVFL.Log("Going to sum " + a + " " + b);
        ThreadVFL.Log("Sum = " + a + b);
        return a + b;
    }

    int multiply(int a, int b) {
        ThreadVFL.Log("Multiplying " + a + " and " + b);
        int result = a * b;
        ThreadVFL.Log("Multiplying value = " + result);
        return result;
    }


    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Simple Linear test", buffer, () -> {
                ThreadVFL.Log("This is a log #1");
                ThreadVFL.Log("Now going to start another block");
                int result = ThreadVFL.CallPrimarySubBlock("Sum block", "Doing sum of 1, 2", () -> sum(1, 2), integer -> "Calculated sum is " + integer);
                ThreadVFL.Log("So now the result is " + result);
            });
            write("linearFlow");
        }
    }

    @Nested
    class AsyncFlow {
        @Test
        void asyncTest() {
            ThreadVFL.Runner.Instance.StartVFL("AsyncFlow Test", buffer, () -> {
                ThreadVFL.Log("Starting async test now...");
                int r = ThreadVFL.CallPrimarySubBlock("Sum primary", "Starting primary sum block first", () -> sum(1, 2), integer -> "Result of sum block is " + integer);
                CompletableFuture<Integer> t1 = ThreadVFL.CallSecondaryJoiningBlock("Sum async", "Squaring in async", () -> {
                    ThreadVFL.Log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ThreadVFL.Log("Finished sleeping");
                    return sum(1, 2);
                }, integer -> "Result is " + integer, null);
                CompletableFuture<Integer> t2 = ThreadVFL.CallSecondaryJoiningBlock("Multiply async", "Multiply in async", () -> {
                    ThreadVFL.Log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ThreadVFL.Log("Finished sleeping");
                    return multiply(1, 2);
                }, integer -> "Result is " + integer, Executors.newVirtualThreadPerTaskExecutor());

                Integer a;
                Integer b;
                try {
                    a = t1.get();
                    b = t2.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                Integer finalA = a;
                Integer finalB = b;
                ThreadVFL.CallPrimarySubBlock("Sum primary 2", "Doing sum of results", () -> sum(finalA, finalB), integer -> "Final result = " + integer);
                ThreadVFL.Log("Everything is DONE and dusted!!!");
            });
            write("AsyncFlow");
        }
    }

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Sum listener", "Calculating sum of " + a + " " + b, buffer, eventPublisherBlock, () -> {
                ThreadVFL.Log("Starting event listener of sum");
                int r = a + b;
                ThreadVFL.Log("Sum = " + r);
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Multiply listener", "Multiplying " + a + " and " + b, buffer, eventPublisherBlock, () -> {
                ThreadVFL.Log("Starting event listener of multiply");
                int r = a * b;
                ThreadVFL.Log("Multiply = " + r);
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Linear event publisher and listener test", buffer, () -> {
                ThreadVFL.Log("Starting event publisher and listener test");
                var publisherBlock = ThreadVFL.CreateEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);
                ThreadVFL.Log("Published event now closing");
            });
            write("linearEventFlow");
        }
    }
}