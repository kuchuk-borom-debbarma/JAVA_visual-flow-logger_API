package threadVfl;

import dev.kuku.vfl.ThreadVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ThreadVFLFluent {

    private final InMemoryFlushHandlerImpl flush = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(999999, 99999, flush);

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
        var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
        fluentLogger.logText("Going to sum " + a + " " + b).asMessage();
        fluentLogger.logText("Sum = " + (a + b)).asMessage();
        return a + b;
    }

    int multiply(int a, int b) {
        var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
        fluentLogger.logText("Multiplying " + a + " and " + b).asMessage();
        int result = a * b;
        fluentLogger.logText("Multiplying value = " + result).asMessage();
        return result;
    }

    @Nested
    class LinearFlow {
        @Test
        void linearFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Simple Linear test", buffer, () -> {
                var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
                fluentLogger.logText("This is a log #1").asMessage();
                fluentLogger.logText("Now going to start another block").asMessage();

                int result = fluentLogger.startSubBlock("Sum block")
                        .withStartMessage("Doing sum of 1, 2")
                        .forCallable(() -> sum(1, 2))
                        .withEndMessageSerializer(integer -> "Calculated sum is " + integer)
                        .executeAsPrimary();

                fluentLogger.logText("So now the result is " + result).asMessage();
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
                var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
                fluentLogger.logText("Starting async test now...").asMessage();

                int r = fluentLogger.startSubBlock("Sum primary")
                        .withStartMessage("Starting primary sum block first")
                        .forCallable(() -> sum(1, 2))
                        .withEndMessageSerializer(integer -> "Result of sum block is " + integer)
                        .executeAsPrimary();

                CompletableFuture<Integer> t1 = fluentLogger.startSubBlock("Sum async")
                        .withStartMessage("Squaring in async")
                        .forCallable(() -> {
                            var fluentInnerLogger = new FluentVFLCallable(ThreadVFL.Get());
                            fluentInnerLogger.logText("Sleeping now").asMessage();
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            fluentInnerLogger.logText("Finished sleeping").asMessage();
                            return sum(1, 2);
                        })
                        .withEndMessageSerializer(integer -> "Result is " + integer)
                        .executeAsJoiningSecondary(null);

                CompletableFuture<Integer> t2 = fluentLogger.startSubBlock("Multiply async")
                        .withStartMessage("Multiply in async")
                        .forCallable(() -> {
                            var fluentInnerLogger = new FluentVFLCallable(ThreadVFL.Get());
                            fluentInnerLogger.logText("Sleeping now").asMessage();
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            fluentInnerLogger.logText("Finished sleeping").asMessage();
                            return multiply(1, 2);
                        })
                        .withEndMessageSerializer(integer -> "Result is " + integer)
                        .executeAsJoiningSecondary(null);

                var a = t1.get();
                var b = t2.get();

                fluentLogger.startSubBlock("Sum primary 2")
                        .withStartMessage("Doing sum of results")
                        .forCallable(() -> sum(a, b))
                        .withEndMessageSerializer(integer -> "Final result = " + integer)
                        .executeAsPrimary();

                fluentLogger.logText("Everything is DONE and dusted!!!").asMessage();
                return null;
            });
            write("AsyncFlow");
        }
    }

    @Nested
    class EventFlow {

        void sum(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Sum listener", "Calculating sum of " + a + " " + b, buffer, eventPublisherBlock, () -> {
                var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
                fluentLogger.logText("Starting event listener of sum").asMessage();
                int r = a + b;
                fluentLogger.logText("Sum = " + r).asMessage();
            });
        }

        void multiply(int a, int b, EventPublisherBlock eventPublisherBlock) {
            ThreadVFL.Runner.Instance.StartEventListenerLogger("Multiply listener", "Multiplying " + a + " and " + b, buffer, eventPublisherBlock, () -> {
                var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
                fluentLogger.logText("Starting event listener of multiply").asMessage();
                int r = a * b;
                fluentLogger.logText("Multiply = " + r).asMessage();
            });
        }

        @Test
        void linearEventFlow() {
            ThreadVFL.Runner.Instance.StartVFL("Linear event publisher and listener test", buffer, () -> {
                var fluentLogger = new FluentVFLCallable(ThreadVFL.Get());
                fluentLogger.logText("Starting event publisher and listener test").asMessage();

                var publisherBlock = ThreadVFL.Get().createEventPublisherBlock("On Publish number", "Publishing 2 numbers");
                sum(1, 2, publisherBlock);
                multiply(1, 2, publisherBlock);

                fluentLogger.logText("Published event now closing").asMessage();
                return null;
            });
            write("linearEventFlow");
        }
    }
}
