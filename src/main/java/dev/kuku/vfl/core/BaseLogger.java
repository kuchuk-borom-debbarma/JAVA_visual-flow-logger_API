package dev.kuku.vfl.core;

/**
 * Simple logger for logging strings.
 */
public interface BaseLogger {
    void text(String message);

    void textHere(String message);

    void warn(String message);

    void warnHere(String message);

    void error(String message);

    void errorHere(String message);


    void closeBlock(String endMessage);
}
//TODO Execution logger
//TODO Thread safe async logger using virtual threads
//TODO local file flush handler
//TODO annotation based flow logger
//TODO figure out how we can display forked block which joins back
//TODO take in list of flushHandler and flush to all of them