import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.threadLocal.IThreadLocal;
import dev.kuku.vfl.threadLocal.ThreadLocaVFL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

public class ThreadLocalVFLTest {

    InMemoryFlushHandlerImpl memory = new InMemoryFlushHandlerImpl();
    VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, memory);

    @AfterEach
    void after() throws IOException {
        try (FileWriter f = new FileWriter("nested.json")) {
            f.write(memory.toJsonNested());
        }
    }

    @Test
    void nestedTest() {
        IThreadLocal.Runner.call("Nested test", buffer, () -> {
            var l = ThreadLocaVFL.get();
            l.msg("Hello how is everyone");
            int nested = l.call("Sum block", "Calculating sum", this::sum, integer -> "nested = " + integer);
            l.msg("after nest value is " + nested);
            return null;
        });
    }

    int sum() {
        var l = ThreadLocaVFL.get();
        l.msg("Sum is 123");
        return 123;
    }
}