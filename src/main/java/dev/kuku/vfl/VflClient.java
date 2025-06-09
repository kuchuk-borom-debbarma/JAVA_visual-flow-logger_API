package dev.kuku.vfl;


import java.util.UUID;
import java.util.function.Function;

public class VflClient {
    private final VflBuffer buffer;

    public VflClient(VflBuffer buffer) {
        this.buffer = buffer;
    }

    public <T> T startRootBlock(String blockName, Function<VflBlockOperator, T> fn) {
        String id = UUID.randomUUID().toString();
        var block = buffer.createBlock(null, id, blockName);
        return fn.apply(block);
    }

}
