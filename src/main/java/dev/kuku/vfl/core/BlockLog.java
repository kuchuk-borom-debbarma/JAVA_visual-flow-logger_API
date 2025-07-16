package dev.kuku.vfl.core;

/**
 * Simple logger for logging strings.
 */
public interface BlockLog {
    void text(String message);

    void textHere(String message);

    void warn(String message);

    void warnHere(String message);

    void error(String message);

    void errorHere(String message);

    void run(String blockName, String message, Runnable runnable);

    void runHere(String blockName, String message, Runnable runnable);

    //TODO callables

    void closeBlock(String endMessage);
}
