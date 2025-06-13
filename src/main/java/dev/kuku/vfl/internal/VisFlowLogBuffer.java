package dev.kuku.vfl.internal;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;

public interface VisFlowLogBuffer {
    void pushLogToBuffer(VflLogDataType log);

    void pushBlockToBuffer(VflBlockDataType subBlock);
}
