package passthrough;

import dev.kuku.vfl.IPassthroughVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.fluent.PassthroughFluent;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PassFluentTest {
    private final InMemoryFlushHandlerImpl inMemoryFlushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(1, 1, inMemoryFlushHandler);

    void write(String fileName) {
        String path = "test/output/passthrough/fluent";
        try {
            Files.createDirectories(Path.of(path));
            try (var f = new FileWriter(path + "/" + fileName + ".json")) {
                f.write(inMemoryFlushHandler.toJsonNested());
                inMemoryFlushHandler.cleanup();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void flat() {
        IPassthroughVFL.PassthroughVFLRunner.run("flat test", buffer, (l) -> {
            var f = new PassthroughFluent(l);
            f.logText("Starting flat test").asMsg();
            f.logText("So this is a flat").asError();
        });
        write("flat");
    }

    @Test
    void nested() {
        IPassthroughVFL.PassthroughVFLRunner.run("flat test", buffer, (l) -> {
            var f = new PassthroughFluent(l);
            f.logText("Starting flat test").asMsg();
            f.logText("Starting a nested call").asError();
            f.startBlock("Sum")
                    .withMsg("Going to calculate sum")
                    .toCall(iPassthroughVFL -> sum(1, 2, iPassthroughVFL))
                    .withEndMsg(integer -> "Returned value is " + integer)
                    .call();
        });
        write("nested");
    }

    int sum(int a, int b, IPassthroughVFL logger) {
        var f = new PassthroughFluent(logger);
        f.logText("Summing " + a + " and " + b);
        return f.fn(() -> a + b)
                .asMsg(integer -> "sum = " + integer);
    }
}