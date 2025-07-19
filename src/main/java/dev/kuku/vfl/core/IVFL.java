package dev.kuku.vfl.core;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Simple logger for logging strings.
 */
public interface IVFL {
    void msg(String message);

    <R> R msgFn(Callable<R> fn, Function<R, String> messageFn);

    void warn(String message);

    <R> R warnFn(Callable<R> fn, Function<R, String> messageFn);

    void error(String message);

    <R> R errorFn(Callable<R> fn, Function<R, String> messageFn);

    void closeBlock(String endMessage);
}
//TODO Thread safe async logger using virtual threads. The one i amde rn is very buggy and needs to be redone
//TODO local file flush handler
//TODO annotation based flow logger
//TODO figure out how we can display forked block which joins back
//TODO take in list of flushHandler and flush to all of them using new flush type
//TODO different level for filtering
//TODO common class for simple stuffs
//TODO compile time flow generation for flow chart