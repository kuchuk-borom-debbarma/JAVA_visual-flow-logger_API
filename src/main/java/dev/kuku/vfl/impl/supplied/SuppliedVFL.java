package dev.kuku.vfl.impl.supplied;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

public class SuppliedVFL extends VFLFn {

    private final VFLBlockContext loggerContext;

    public SuppliedVFL(VFLBlockContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    protected SuppliedVFL getSubBlockLogger(Block subBlock, SubBlockStartLog subBlockStartLog) {
        return new SuppliedVFL(new VFLBlockContext(subBlock, loggerContext.buffer));
    }

    @Override
    protected void initializeSubBlockInImplementation(VFLBlockContext executionContext, Block createdSubBlock, SubBlockStartLog subBlockStartLog) {
        //Nothing that needs to be done
    }

    @Override
    protected void setupAsyncSubBlockContext(Block subBlock, Log subBlockStartLog) {
        //Nothing that needs to be done
    }

    @Override
    protected VFLBlockContext getContext() {
        return loggerContext;
    }
}
