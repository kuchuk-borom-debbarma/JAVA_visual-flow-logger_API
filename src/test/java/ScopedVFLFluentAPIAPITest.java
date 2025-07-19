import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scoped.IScopedVFL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

import static dev.kuku.vfl.scoped.ScopedFluentAPI.subBlockRun;
import static dev.kuku.vfl.scoped.ScopedFluentAPI.text;

public class ScopedVFLFluentAPIAPITest {

    InMemoryFlushHandlerImpl inMemory = new InMemoryFlushHandlerImpl();
    VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, inMemory);

    @BeforeEach
    void beforeEach() {
        inMemory.cleanup();
    }

    @AfterEach
    void afterEach() {
        //write to file
        try (var f = new FileWriter("nested.json")) {
            System.out.println("Writing result to file");
            f.write(inMemory.toJsonNested());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void test() {
        IScopedVFL.Runner.run("Scoped FluentAPI", this.buffer, () -> {
            text.msg("Starting test");
            subBlockRun(() -> sum(1, 2))
                    .withMsg("Summing two numbers")
                    .run();
            text.msg("Scoped fluent test complete");
        });
    }

    void sum(int a, int b) {
        text.msg("Sum of " + a + " and " + b);
        int sum = text.fn(() -> a + b).msg(integer -> "sum = " + integer);
    }
}
