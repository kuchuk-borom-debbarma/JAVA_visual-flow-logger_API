package dev.kuku.vfl.internal;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;

import java.util.concurrent.CompletableFuture;

public interface VisFlowLogBuffer {
    CompletableFuture<Void> pushLogToBuffer(VflLogDataType log);

    CompletableFuture<Void> pushBlockToBuffer(VflBlockDataType subBlock);
}
