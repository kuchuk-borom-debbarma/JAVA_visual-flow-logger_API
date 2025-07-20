import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.VFLFluentAPI;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
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
            fluent.logText("Hello world how is everyone").asMsg();
            int sum = fluent.fn(() -> 1 + 2).asMsg(s -> "Sum = " + s);
            return null;
        });

    }
}