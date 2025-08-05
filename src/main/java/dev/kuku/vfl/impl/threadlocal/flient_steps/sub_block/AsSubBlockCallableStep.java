package dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.ThreadVFL;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;
import static dev.kuku.vfl.core.helpers.Util.UpdateEndMsg;


@RequiredArgsConstructor
public class AsSubBlockCallableStep<R> {
    private final Supplier<R> fn;
    private final String blockName;
    private String startMessage;
    private Function<R, String> endMessage;


    public AsSubBlockCallableStep<R> withStartMessage(String startMessage, Object... args) {
        this.startMessage = FormatMessage(startMessage, args);
        return this;
    }

    public AsSubBlockCallableStep<R> withEndMessage(Function<R, String> endMessage, Object... args) {
        this.endMessage = UpdateEndMsg(endMessage, args);
        return this;
    }

    public AsSubBlockCallableStep<R> withEndMessage(String endMessage, Object... args) {

        this.endMessage = (r) -> {
            Object[] allArgs = Util.combineArgsWithReturn(args, r);
            return Util.FormatMessage(endMessage, allArgs);
        };
        return this;
    }


    public R execute() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, endMessage);
    }

    public R executeDetached() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage);
    }

    public R executeFork() {
        return ThreadVFL.getCurrentLogger().supply(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage);
    }

}