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
    private <R> R callHandler(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
        ensureBlockStarted();
        //Create and push block
        Block subBlock = VFLHelper.CreateBlockAndPushT2Buffer(blockName, getParentLogId(), getBuffer());
        //Create and push log of sub block start type
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(getBlockId(), getParentLogId(), startMessage, subBlock.getId(), logType, getBuffer());
        //Update the log flow chain
        setCurrentLogId(log.getId());
        //Run the callable and incase of any error log it, once done close the logger
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    public final <R> R callPrimarySubBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_PRIMARY);
    }

    public final <R> R callSecondaryJoiningBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_SECONDARY_JOIN);
    }

    public final <R> R callSecondaryNonJoiningBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        return this.callHandler(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    abstract VFLCallable getLogger();
}