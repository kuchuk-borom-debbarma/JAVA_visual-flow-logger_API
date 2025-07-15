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
}
