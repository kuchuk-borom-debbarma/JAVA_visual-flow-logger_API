package passthrough;

import dev.kuku.vfl.PassthroughVFLRunner;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PassthroughTest {
    private final InMemoryFlushHandlerImpl inMemoryFlushHandler = new InMemoryFlushHandlerImpl();
    private final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(1, 1, inMemoryFlushHandler);

    void write(String fileName) {
        String path = "test/output/passthrough/logger";
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
    void level1Nest() {
        PassthroughVFLRunner.run("level1Nest", buffer, l -> {
            l.msg("Starting a level 1 nest test");
            l.msg("Currently at level 1");
            String name = l.call("Name generator", "Generating name in another block",
                    sl -> {
                        sl.msg("Generating name,,,,,");
                        sl.msg("Generated name is kuku");
                        return "kuku";
                    }, s -> "The function returned name " + s);
            l.msg("Hello " + name + "!");
            l.run("Second function", "Starting second fn to test", sl -> {
                sl.msg("Another fn called");
                sl.msg("Just testing out the run method here");
            });
            l.msg("finished");
        });
        write("level1Nest");
    }

    @Test
    void multiNest() {
        PassthroughVFLRunner.run("level1Nest", buffer, l -> {
            l.msg("Starting a level 1 nest test");
            l.msg("Currently at level 1");
            String name = l.call("Name generator", "Generating name in another block",
                    sl -> {
                        sl.msg("Generating name,,,,,");
                        sl.msg("Generated name is kuku");

                        sl.run("inner function", "Starting inner fn", sll -> {
                            sll.msg("Another fn called");
                            sll.msg("Just testing out the run method here");
                            String a = sll.call("thirdFn", null, iPassthroughVFL -> {
                                iPassthroughVFL.msg("This is third fn");
                                return iPassthroughVFL.msgFn(() -> "GGEZ", s -> "Returning ggez");
                            }, s -> "Got " + s + " From third fn ");
                            sll.msg("thirdFnData = " + a);
                        });
                        return "kuku";
                    }, s -> "The function returned name " + s);
            l.msg("Hello " + name + "!");
            l.run("Second function", "Starting second fn to test", sl -> {
                sl.msg("Another fn called");
                sl.msg("Just testing out the run method here");
            });
            l.msg("finished");
        });
        write("multiNest");
    }

    void asyncTest() {
    }
}