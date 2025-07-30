package threadVfl;

import dev.kuku.vfl.FluentThreadVFL;
import dev.kuku.vfl.ThreadVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadVFLFluentTest {

    private final ThreadSafeInMemoryFlushHandlerImpl flush = new ThreadSafeInMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(999999, flush);
    private FluentVFLCallable f;

    void write(String fileName) {
        try {
            String path = "test/output/threadVFL/fluent";
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
        var f = FluentThreadVFL.Get();
        f.log("Going to sum " + a + " " + b);
        return f.call(() -> a + b)
                .asLog(integer -> "Sum = " + integer);
    }

    int multiply(int a, int b) {
        var f = FluentThreadVFL.Get();
        f.log("Multiplying " + a + " and " + b);
        return f.call(() -> a * b)
                .asLog(i -> "Multiplied value = " + i);
    }

    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Simple Linear test", buffer, () -> {
                f = FluentThreadVFL.Get();
                f.log("This is a log #1");
                f.log("Now going to start another block");

                int result = f.call(() -> sum(1, 2))
                        .asSubBlock("Multiply block")
                        .withStartMessage("Doing sum of 1, 2")
                        .withEndMessageMapper(o -> "Result is " + o)
                        .startPrimary();
                f.log("So now the result is " + result);
                return null;
            });
            write("linearFlow");
        }
    }

    @Nested
    class AsyncFlow {
        @Test
        void asyncTest() {
            ThreadVFL.Runner.Instance.StartVFL("AsyncFlow Test", buffer, () -> {
                f = FluentThreadVFL.Get();
                f.log("Starting async test now...");

                f.runSubBlock(() -> sum(1, 2))
                        .withBlockName("Sum Primary 1")
                        .startPrimary();

                CompletableFuture<Integer> t1 = f.call(() -> {
                            try {
                                var f = FluentThreadVFL.Get();
                                f.log("Sleeping sum now");
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            f.log("Sleeping sum finished now calculating actual sum...");
                            return sum(1, 2);
                        })
                        .asSubBlock("Sum async")
                        .withStartMessage("Starting sum async block")
                        .withEndMessageMapper(integer -> "After sleeping sum is " + integer)
                        .startSecondaryJoining(null);

                CompletableFuture<Integer> t2 = f.call(() -> {
                            try {
                                var f = FluentThreadVFL.Get();
                                f.log("Sleeping multiply now");
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            f.log("Sleeping multiply finished now calculating actual sum...");
                            return multiply(1, 2);
                        })
                        .asSubBlock("Multiply async")
                        .withStartMessage("Starting Multiply async block")
                        .withEndMessageMapper(integer -> "After sleeping multiply is " + integer)
                        .startSecondaryJoining(Executors.newVirtualThreadPerTaskExecutor());

                try {
                    var a = t1.get();
                    var b = t2.get();
                    f.log("Final a and b after async operation is " + a + " and " + b);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                try {
                    f.log("Running async non joining SUM now");
                    f.runSubBlock(() -> sum(1, 2))
                            .withBlockName("Non Joining secondary")
                            .startSecondaryNonJoining(null).get();
                    f.log("Async non joining SUM complete");
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                f.log("Everything is DONE and dusted!!!");
                return null;
            });
            write("AsyncFlow");
        }
    }

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Sum listener", "Calculating sum of " + a + " " + b, buffer, eventPublisherBlock, () -> {
                f.log("Starting event listener of sum");
                int r = a + b;
                f.log("Sum = " + r);
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Multiply listener", "Multiplying " + a + " and " + b, buffer, eventPublisherBlock, () -> {
                f.log("Starting event listener of multiply");
                int r = a * b;
                f.log("Multiply = " + r);
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Linear event publisher and listener test", buffer, () -> {
                f = FluentThreadVFL.Get();
                f.log("Starting event publisher and listener test");

                var publisherBlock = ThreadVFL.CreateEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);

                f.log("Published event now closing");
                return null;
            });
            write("linearEventFlow");
        }
    }
}
