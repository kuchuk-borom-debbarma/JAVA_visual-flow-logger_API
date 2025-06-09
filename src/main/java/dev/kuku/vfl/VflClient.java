package dev.kuku.vfl;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public record VflClient(VflBuffer buffer) {

    /**
     * Starts a root block for operations that return a value
     *
     * @param <T>       The return type of the operation
     * @param blockName Name of the root block
     * @param fn        Operation to execute that returns a value
     * @return The result of the operation
     */
    public <T> T startRootBlock(String blockName, Function<ImmutableVflBlockOperator, T> fn) {
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
    public void startRootBlock(String blockName, Consumer<ImmutableVflBlockOperator> fn) {
        String id = UUID.randomUUID().toString();
        var block = buffer.createBlock(null, id, blockName);
        fn.accept(block);
    }

    public ImmutableVflBlockOperator startSubBlock(String blockName) {
        return buffer.createBlock(null, UUID.randomUUID().toString(), blockName);
    }
}