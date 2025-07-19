import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.core.fluent.VFLFluentAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

public class VFLFluentAPITest {
    private final InMemoryFlushHandlerImpl flushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, flushHandler);

    @AfterEach
    void afterEach() throws IOException {
        buffer.flushAndClose();
        try (var f = new FileWriter("nested.json")) {
            f.write(flushHandler.toJsonNested());
        }
    }

    @Test
    void test() {
        IVFL.Runner.call("TEST", buffer, ivfl -> {
            var fluent = new VFLFluentAPI(ivfl);
            fluent.msg("Logging some stuff");
            int sum = fluent.fn(() -> 1 + 2)
                    .textFn(integer -> String.format("Result is %s", integer)).msg();
            square(10, ivfl);
            return null;
        });

    }

    int square(int num, IVFL logger) {
        var fluent = new VFLFluentAPI(logger);
        fluent.msg("Squaring " + num);
        return fluent.fn(() -> num * num).textFn(integer -> String.format("Square of %s is %s", num, integer)).msg();
    }
}