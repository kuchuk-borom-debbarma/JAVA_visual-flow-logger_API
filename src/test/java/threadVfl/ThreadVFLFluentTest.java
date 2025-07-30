package threadVfl;

import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import dev.kuku.vfl.variants.thread_local.FluentThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;
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
        FluentThreadVFL.Log("Going to sum " + a + " " + b);
        return FluentThreadVFL.Call(() -> a + b)
                .asLog(integer -> "Sum = " + integer);
    }

    int multiply(int a, int b) {
        FluentThreadVFL.Log("Multiplying " + a + " and " + b);
        return FluentThreadVFL.Call(() -> a * b)
                .asLog(i -> "Multiplied value = " + i);
    }

    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Simple Linear test", buffer, () -> {
                FluentThreadVFL.Log("This is a log #1");
                FluentThreadVFL.Log("Now going to start another block");

                int result = FluentThreadVFL.Call(() -> sum(1, 2))
                        .asSubBlock("Multiply block")
                        .withStartMessage("Doing sum of 1, 2")
                        .withEndMessageMapper(o -> "Result is " + o)
                        .startPrimary();
                FluentThreadVFL.Log("So now the result is " + result);
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
                FluentThreadVFL.Log("Starting async test now...");

                FluentThreadVFL.RunSubBlock(() -> sum(1, 2))
                        .withBlockName("Sum Primary 1")
                        .startPrimary();

                CompletableFuture<Integer> t1 = FluentThreadVFL.Call(() -> {
                            try {
                                FluentThreadVFL.Log("Sleeping sum now");
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            FluentThreadVFL.Log("Sleeping sum finished now calculating actual sum...");
                            return sum(1, 2);
                        })
                        .asSubBlock("Sum async")
                        .withStartMessage("Starting sum async block")
                        .withEndMessageMapper(integer -> "After sleeping sum is " + integer)
                        .startSecondaryJoining(null);

                CompletableFuture<Integer> t2 = FluentThreadVFL.Call(() -> {
                            try {
                                FluentThreadVFL.Log("Sleeping multiply now");
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            FluentThreadVFL.Log("Sleeping multiply finished now calculating actual sum...");
                            return multiply(1, 2);
                        })
                        .asSubBlock("Multiply async")
                        .withStartMessage("Starting Multiply async block")
                        .withEndMessageMapper(integer -> "After sleeping multiply is " + integer)
                        .startSecondaryJoining(Executors.newVirtualThreadPerTaskExecutor());

                try {
                    var a = t1.get();
                    var b = t2.get();
                    FluentThreadVFL.Log("Final a and b after async operation is " + a + " and " + b);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                try {
                    FluentThreadVFL.Log("Running async non joining SUM now");
                    FluentThreadVFL.RunSubBlock(() -> sum(1, 2))
                            .withBlockName("Non Joining secondary")
                            .startSecondaryNonJoining(null).get();
                    FluentThreadVFL.Log("Async non joining SUM complete");
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                FluentThreadVFL.Log("Everything is DONE and dusted!!!");
                return null;
            });
            write("AsyncFlow");
        }
    }

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Sum listener", "Calculating sum of " + a + " " + b, buffer, eventPublisherBlock, () -> {
                FluentThreadVFL.Log("Starting event listener of sum");
                int r = a + b;
                FluentThreadVFL.Log("Sum = " + r);
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Multiply listener", "Multiplying " + a + " and " + b, buffer, eventPublisherBlock, () -> {
                FluentThreadVFL.Log("Starting event listener of multiply");
                int r = a * b;
                FluentThreadVFL.Log("Multiply = " + r);
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Linear event publisher and listener test", buffer, () -> {
                FluentThreadVFL.Log("Starting event publisher and listener test");

                var publisherBlock = ThreadVFL.CreateEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);

                FluentThreadVFL.Log("Published event now closing");
                return null;
            });
            write("linearEventFlow");
        }
    }
}
