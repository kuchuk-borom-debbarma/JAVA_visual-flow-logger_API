package vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.fluent.VFLFluent;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FluentTest {
    private final InMemoryFlushHandlerImpl inMemoryFlushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(1, 1, inMemoryFlushHandler);

    void write(String fileName) {
        String path = "test/output/VflTest/fluent";
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
        VFLRunner.run("Fluent test", buffer, ivfl -> {
            var fluent = new VFLFluent(ivfl);
            fluent.logText("What is up!!").asMsg();
            String name = fluent.fn(() -> "Kuku").asMsg(s -> "Hello " + s);
            fluent.logText("Name is " + name).asMsg();
        });
        write("flat");
    }

    @Test
    void errTest() {
        try {
            VFLRunner.run("Fluent test", buffer, ivfl -> {
                var fluent = new VFLFluent(ivfl);
                fluent.logText("What is up!!").asMsg();
                String name = fluent.fn(() -> "Kuku").asMsg(s -> "Hello " + s);
                fluent.logText("Name is " + name).asMsg();
                throw new RuntimeException("WTF");
            });
        } catch (RuntimeException _) {
        }
        write("errTest");
    }

    @Test
    void fluentException() {
        try {
            VFLRunner.run("Fluent test", buffer, ivfl -> {
                var fluent = new VFLFluent(ivfl);
                fluent.logText("What is up!!").asMsg();
                String name = fluent.fn(() -> {
                    throw new RuntimeException("GGEZ");
                }).asMsg(s -> "Hello " + s).toString();
                fluent.logText("Name is " + name).asMsg();
            });
        } catch (RuntimeException _) {
        }
        write("fluentException");
    }
}