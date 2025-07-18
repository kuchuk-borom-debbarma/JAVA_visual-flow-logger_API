import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

import static dev.kuku.vfl.scopedVFLogger.fluentApi.ScopedFluent.msg;
import static dev.kuku.vfl.scopedVFLogger.fluentApi.ScopedFluent.subBlockRunner;

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
            msg("This is a simple message");
            subBlockRunner(() -> sum(1, 2))
                    .withMsg("Summing two numbers")
                    .run();
            msg("Scoped fluent test complete");
        });
    }

    void sum(int a, int b) {
        msg("Sum of " + a + " and " + b);
        int sum = a + b;
        msg("Summed = " + sum);
    }

}
