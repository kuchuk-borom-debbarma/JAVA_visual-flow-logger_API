package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step.SetBlockNameNoEndMsg;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;

public class ConsumerSubBlockStartStep extends BaseSubBlockStartStep<Void> {
    private final Consumer<VFLFn> originalConsumer;

    public ConsumerSubBlockStartStep(Consumer<VFLFn> consumer, VFLFn vfl) {
        super(l -> {
            consumer.accept(l);
            return null;
        }, vfl);
        this.originalConsumer = consumer;
    }

    public SetBlockNameNoEndMsg withBlockName(String blockName) {
        return new SetBlockNameNoEndMsg(blockName, vfl, originalConsumer);
    }
}