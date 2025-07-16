package dev.kuku;


import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.serviceCall.InMemoryVFLApiImpl;
import dev.kuku.vfl.scopedValue.ScopedBlockLoggerStarter;

public class Main {
    static final InMemoryVFLApiImpl inMemory = new InMemoryVFLApiImpl();
    static final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, inMemory);
    private static final BlockLog logger = ScopedBlockLoggerStarter.start("Main", buffer); //Passing null for now

    public static void main(String... args) {
        logger.textHere("Starting main");
        logger.run(Main::root, "Root", "Starting root");
        buffer.shutdown();
        System.out.println(inMemory.blocks);
        System.out.println(inMemory.logs);
    }

    static void root() {
        logger.text("Root started");
        logger.run(Main::nested, "Nested", "Starting Nested operation");
    }

    static void nested() {
        logger.text("IDK");
    }
}


//TODO better safety using builder for everything