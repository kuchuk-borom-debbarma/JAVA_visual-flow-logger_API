package dev.kuku.vfl;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VflClient {
    public final VflBuffer buffer;

    public VflClient(VflBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Starts a root block for operations that return a value
     *
     * @param <T>       The return type of the operation
     * @param blockName Name of the root block
     * @param fn        Operation to execute that returns a value
     * @return The result of the operation
     */
    public <T> T startRootBlock(String blockName, Function<VflBlockOperator, T> fn) {
        String id = UUID.randomUUID().toString();
        var block = buffer.createBlock(null, id, blockName);
        return fn.apply(block);
    }

    /**
     * Starts a root block for void operations
     *
     * @param blockName Name of the root block
     * @param fn        Void operation to execute
     */
    public void startRootBlock(String blockName, Consumer<VflBlockOperator> fn) {
        String id = UUID.randomUUID().toString();
        var block = buffer.createBlock(null, id, blockName);
        fn.accept(block);
    }
}