package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;

import java.util.function.Function;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum.*;

public abstract class VFLFn extends VFL {
    private <R> R fnHandler(String blockName, String startMessage, Function<VFLFn, R> fn, Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
        ensureBlockStarted();
        Block subBlock = VFLHelper.CreateBlockAndPushT2Buffer(blockName, getCurrentLogId(), getBuffer());
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(getBlockId(), getCurrentLogId(), startMessage, subBlock.getId(), logType, getBuffer());
        setCurrentLogId(log.getId());
        return VFLHelper.CallFnWithLogger(() -> fn.apply(getLogger()), getLogger(), endMessageSerializer);
    }

    public final <R> R callPrimarySubBlock(String blockName, String startMessage, Function<VFLFn, R> fn, Function<R, String> endMessageSerializer) {
        return this.fnHandler(blockName, startMessage, fn, endMessageSerializer, SUB_BLOCK_START_PRIMARY);
    }

    public final <R> R callSecondaryJoiningBlock(String blockName, String startMessage, Function<VFLFn, R> fn, Function<R, String> endMessageSerializer) {
        return this.fnHandler(blockName, startMessage, fn, endMessageSerializer, SUB_BLOCK_START_SECONDARY_JOIN);
    }

    public final <R> R callSecondaryNonJoiningBlock(String blockName, String startMessage, Function<VFLFn, R> fn, Function<R, String> endMessageSerializer) {
        return this.fnHandler(blockName, startMessage, fn, endMessageSerializer, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    abstract protected VFLFn getLogger();
}