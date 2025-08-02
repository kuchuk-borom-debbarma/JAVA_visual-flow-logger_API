package threadVfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadVFLTest {

    private final NestedJsonFlushHandler flush = new NestedJsonFlushHandler("test/output/threadVFL/logger.json");
    private final VFLBuffer buffer = new AsyncVFLBuffer(100, 5000, 5000, flush, Executors.newVirtualThreadPerTaskExecutor(), Executors.newScheduledThreadPool(2));

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
            ThreadVFL.Runner.Instance.startVFL("Simple Linear test", buffer, () -> {
                ThreadVFL.Log("This is a log #1");
                ThreadVFL.Log("Now going to start another block");
                int result = ThreadVFL.CallPrimarySubBlock("Sum block", "Doing sum of 1, 2", () -> sum(1, 2), integer -> "Calculated sum is {}");
                ThreadVFL.Log("So now the result is " + result);
            });
        }
    }

    @Nested
    class AsyncFlow {
        @Test
        void asyncTest() {
            ThreadVFL.Runner.Instance.startVFL("AsyncFlow Test", buffer, () -> {
                ThreadVFL.Log("Starting async test now...");
                int r = ThreadVFL.CallPrimarySubBlock("Sum primary", "Starting primary sum block first", () -> sum(1, 2), integer -> "Result of sum block is {}");
                CompletableFuture<Integer> t1 = ThreadVFL.CallSecondaryJoiningBlock("Sum async", "Squaring in async", () -> {
                    ThreadVFL.Log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ThreadVFL.Log("Finished sleeping");
                    return sum(1, 2);
                }, integer -> "Result is {}");
                CompletableFuture<Integer> t2 = ThreadVFL.CallSecondaryJoiningBlock("Multiply async", "Multiply in async", () -> {
                    ThreadVFL.Log("Sleeping now");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ThreadVFL.Log("Finished sleeping");
                    return multiply(1, 2);
                }, integer -> "Result is {}", Executors.newVirtualThreadPerTaskExecutor());

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
        }
    }

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.startEventListenerLogger("Sum listener", "Calculating sum of " + a + " " + b, buffer, eventPublisherBlock, () -> {
                ThreadVFL.Log("Starting event listener of sum");
                int r = a + b;
                ThreadVFL.Log("Sum = " + r);
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.startEventListenerLogger("Multiply listener", "Multiplying " + a + " and " + b, buffer, eventPublisherBlock, () -> {
                ThreadVFL.Log("Starting event listener of multiply");
                int r = a * b;
                ThreadVFL.Log("Multiply = " + r);
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Instance.startVFL("Linear event publisher and listener test", buffer, () -> {
                ThreadVFL.Log("Starting event publisher and listener test");
                var publisherBlock = ThreadVFL.CreateEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);
                ThreadVFL.Log("Published event now closing");
            });
        }
    }
}