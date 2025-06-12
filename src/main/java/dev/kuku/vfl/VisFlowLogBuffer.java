package dev.kuku.vfl;

public interface VisFlowLogBuffer {
    void pushLogToBuffer(VflLogDataType log);

    void pushBlockToBuffer(VflBlockDataType subBlock);
}
