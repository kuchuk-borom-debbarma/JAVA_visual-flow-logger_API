import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scoped.IScopedVFL;
import dev.kuku.vfl.scoped.ScopedFluentAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ScopedFluentAPITest {
    private final InMemoryFlushHandlerImpl flushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, flushHandler);

    @AfterEach
    void end() throws IOException {
        try (var f = new FileWriter("nested.json")) {
            f.write(flushHandler.toJsonNested());
        }
    }

    @Test
    void test() {
        IScopedVFL.Runner.run("Scoped VFL Fluent API Test", buffer, () -> {
            var fluent = ScopedFluentAPI.get();
            fluent.logText("Starting scoped vfl fluent test").asMsg();
            int a = fluent.fn(() -> 3).asMsg(i -> "int a = " + i);
            int b = fluent.startBlock("Sum block")
                    .withMsg("Setting b to square of " + a)
                    .andCall(() -> square(a))
                    .withEndMsg(integer -> "b = " + integer)
                    .call();
            int c = fluent.startBlock("Both block")
                    .andCall(() -> both(b))
                    .withEndMsg(integer -> "C = " + integer)
                    .call();
            fluent.logText("Complete test").asMsg();
        });
    }

    int sum(int a, int b) {
        var fluent = ScopedFluentAPI.get();
        fluent.logText("sum of " + a + " and " + b).asMsg();
        return fluent.fn(() -> a + b).asMsg(integer -> "Calculated sum is " + integer);
    }

    int square(int a) {
        var fluent = ScopedFluentAPI.get();
        fluent.logText("Square of " + a).asMsg();
        return fluent.fn(() -> a * a).asMsg(integer -> "Square(" + a + ") = " + integer);
    }

    int both(int a) throws ExecutionException, InterruptedException {
        var fluent = ScopedFluentAPI.get();
        var futureSum = fluent.startBlock("Future sum block")
                .withMsg("Future sum of " + a)
                .asAsync()
                .withExecutor(Executors.newVirtualThreadPerTaskExecutor())
                .andCall(() -> sum(a, a)).withEndMsg(integer -> "Calculated future sum is " + integer)
                .call();
        var futureSquare = fluent.startBlock("Future square block")
                .asAsync()
                .andCall(() -> square(a))
                .call();
        var s = futureSum.get();
        var sq = futureSquare.get();
        return fluent.fn(() -> s + sq).asMsg(i -> String.format("both of %s is %s", a, i));
    }
}