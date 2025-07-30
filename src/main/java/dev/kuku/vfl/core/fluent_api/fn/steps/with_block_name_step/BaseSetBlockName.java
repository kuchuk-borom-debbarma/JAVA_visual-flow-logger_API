package dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockStartMsg;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;
import lombok.RequiredArgsConstructor;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

@RequiredArgsConstructor
public abstract class BaseSetBlockName implements BlockStartMsg {
    protected final String blockName;
    protected final VFLFn vfl;
    protected String startMessage = null;

    @Override
    public BaseSetBlockName withStartMessage(String startMessage, Object... args) {
        this.startMessage = FormatMessage(startMessage, args);
        return this;
    }
}