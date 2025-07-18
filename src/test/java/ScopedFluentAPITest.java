import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import dev.kuku.vfl.scopedVFLogger.fluentApi.ScopedFluentAPI;
import dev.kuku.vfl.scopedVFLogger.fluentApi.ScopeFluentVFL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

public class ScopedFluentAPITest {
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
        ScopedVFLRunner.run("Scoped FluentAPI", this.buffer, () -> {
            new ScopedFluentAPI().msg("This is a simple message");
            new ScopedFluentAPI()
                    .subBlock().blockName("Sum sub block")
                    .blockMsg("Summing two numbers")
                    .run(() -> sum(1, 2));
            new ScopedFluentAPI().msg("Scoped fluent test complete");
        });
    }

    void sum(int a, int b) {
        new ScopedFluentAPI().msg("Sum of " + a + " and " + b);
        int sum = a + b;
        new ScopedFluentAPI().msg("Summed = " + sum);
    }

}
