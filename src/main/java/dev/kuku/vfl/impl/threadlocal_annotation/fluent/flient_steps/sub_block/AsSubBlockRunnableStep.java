package dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.sub_block;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal_annotation.logger.ThreadVFL;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AsSubBlockRunnableStep {
    private final String blockName;
    private final Runnable fn;
    private String startMessage;

    public AsSubBlockRunnableStep withStartMessage(String startMessage, Object... args) {
        this.startMessage = Util.FormatMessage(startMessage, args);
        return this;
    }

    public void execute() {
        ThreadVFL.getCurrentLogger().run(blockName, startMessage, fn);
    }


    public void executeDetached() {
        ThreadVFL.getCurrentLogger().run(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    public void executeFork() {
        ThreadVFL.getCurrentLogger().run(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN);
    }

}
