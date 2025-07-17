package dev.kuku;


import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedLogger.ScopedLogger;
import dev.kuku.vfl.scopedLogger.ScopedLoggerImpl;
import dev.kuku.vfl.scopedLogger.ScopedLoggerRunner;

public class Main {
    static final InMemoryFlushHandlerImpl inMemory = new InMemoryFlushHandlerImpl();
    static final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, inMemory);

    public static void main(String... args) {
        ScopedLoggerRunner.run("Main", buffer, () -> {
            ScopedLogger logger = ScopedLoggerImpl.get();
            logger.text("Starting main");
            logger.text("Doing some stuff in main");
            logger.run("Root", "Starting root call", Main::root);
            logger.text("Finished running main");
        });
        System.out.println(inMemory.blocks.toString());
        System.out.println(inMemory.logs.toString());
        ScopedLoggerImpl.get();
    }

    static void root() {
        var logger = ScopedLoggerImpl.get();
        logger.text("Starting root");
        logger.text("Doing some stuff in root");
        logger.run("Nested", "Starting nested", Main::nested);
        logger.text("Finished running root");
    }

    static void nested() {
        var logger = ScopedLoggerImpl.get();
        logger.text("Starting nested");
        logger.text("Doing some stuff in nested");
    }
}
