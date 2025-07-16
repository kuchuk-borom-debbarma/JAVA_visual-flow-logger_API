package dev.kuku;


import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.ScopedBlockLoggerStarter;

public class Main {
    private static final BlockLog logger = ScopedBlockLoggerStarter.start("Main", null); //Passing null for now

    public static void main(String... args) {
        logger.textHere("Starting main");
        logger.run(Main::root, "Root", "Starting root");
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