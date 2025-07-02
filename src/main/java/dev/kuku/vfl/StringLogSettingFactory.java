package dev.kuku.vfl;

import dev.kuku.vfl.models.VflLogType;

public class StringLogSettingFactory {
    private final BlockLogger blockLogger;
    boolean moveForward = true;
    VflLogType logType = VflLogType.MESSAGE;

    public StringLogSettingFactory(BlockLogger blockLogger) {
        this.blockLogger = blockLogger;
    }

    public StringLogSettingFactory stay() {
        this.moveForward = false;
        return this;
    }

    public StringLogSettingFactory writerType(VflLogType logType) {
        this.logType = logType;
    }


    public void write(String message) {
        blockLogger.addMessageLog(message, logType, moveForward);
    }
}
