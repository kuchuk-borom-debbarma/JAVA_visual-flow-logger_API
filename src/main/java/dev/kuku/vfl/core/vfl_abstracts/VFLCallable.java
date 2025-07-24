package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum.*;

/**
 * Abstract class for VFL that can process callables. <br>
 * It has a getCallableLogger() that needs to be overridden. This is to enable freedom of deciding for how to provide the logger. <br>
 */
public abstract class VFLCallable extends VFL {
    /**
     * Processes a callable inside a sub-block.
     *
     * @param blockName            The name of the sub-block.
     * @param startMessage         The log message to mark block start.
     * @param callable             The operation to run inside the block.
     * @param endMessageSerializer A function that converts the result into a log message when closing.
     * @param logType              The enum defining which log type to use for the sub-block start.
     * @param move                 Whether to update the current log id after creating the sub-block.
     */
    private <R> R callHandler(String blockName,
                              String startMessage,
                              Callable<R> callable,
                              Function<R, String> endMessageSerializer,
                              LogTypeBlcokStartEnum logType,
                              boolean move) {
        var context = getContext();
        ensureBlockStarted();
        Block subBlock = VFLHelper.CreateBlockAndPushT2Buffer(
                blockName,
                context.currentLogId,
                context.buffer
        );

        // Create and push a log denoting sub-block start.
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(
                context.blockInfo.getId(),
                context.currentLogId,
                startMessage,
                subBlock.getId(),
                logType,
                context.buffer
        );

        if (move) {
            context.currentLogId = log.getId();
        }

        // subclasses will decide how to provide a logger.
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    public final <R> R callPrimarySubBlock(String blockName,
                                           String startMessage,
                                           Callable<R> callable,
                                           Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName,
                startMessage,
                callable,
                endMessageSerializer,
                SUB_BLOCK_START_PRIMARY,
                true);
    }

    public final <R> R callSecondaryJoiningBlock(String blockName,
                                                 String startMessage,
                                                 Callable<R> callable,
                                                 Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName,
                startMessage,
                callable,
                endMessageSerializer,
                SUB_BLOCK_START_SECONDARY_JOIN,
                false);
    }

    public final <R> R callSecondaryNonJoiningBlock(String blockName,
                                                    String startMessage,
                                                    Callable<R> callable,
                                                    Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName,
                startMessage,
                callable,
                endMessageSerializer,
                SUB_BLOCK_START_SECONDARY_NO_JOIN,
                false);
    }

    // Subclasses must provide a logger.
    abstract VFLCallable getLogger();
}