package vfl;

import dev.kuku.vfl.IVFL;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VflTest {
    private final InMemoryFlushHandlerImpl inMemoryFlushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(1, 1, inMemoryFlushHandler);

    void write(String fileName) {
        String path = "test/output/VflTest/vfl";
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
    void FlatLogging() {
        IVFL.VFLRunner.run("Flat logging test", buffer, logger -> {
            logger.msg("Testing a flat block right now");
            logger.error("What should i even write lol");
            logger.warn("What should i even write lol");
            int a = logger.msgFn(() -> 1 + 2, integer -> String.format("int a = %s", integer));
        });
        write("flat_logging");
    }

    @Test
    void exceptionThrowing() {
        try {
            IVFL.VFLRunner.call("nested logging test", buffer, l -> {
                int a = l.msgFn(() -> 1, integer -> "a = " + integer);
                int b = l.msgFn(() -> 2, i -> "b = " + i);
                throw new RuntimeException("Wahahah you got pranked!");
            }, o -> String.format("Result of nested logging is %s", o));

        } catch (RuntimeException _) {
        }
        write("exceptionThrowing");
    }
}