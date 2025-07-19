package dev.kuku.vfl.core;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

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

    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Function<IVFL, R> fn) {
            BlockData rootBlock = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlock);
            var rootContext = new VFLBlockContext(rootBlock, buffer);
            var logger = new VFL(rootContext);
            R result;
            try {
                result = fn.apply(logger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                logger.closeBlock(null);
                buffer.flushAndClose();
            }
            return result;
        }
    }
}
//TODO Thread safe async logger using virtual threads. The one i amde rn is very buggy and needs to be redone
//TODO local file flush handler
//TODO annotation based flow logger
//TODO figure out how we can display forked block which joins back
//TODO take in list of flushHandler and flush to all of them using new flush type
//TODO different level for filtering
//TODO common class for simple stuffs
//TODO compile time flow generation for flow chart