package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

public abstract class VFL {
    //TODO consider allowed log types
    public final void log(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getBlockId(), getCurrentLogId(), LogTypeEnum.MESSAGE, mesage, getBuffer());
        setCurrentLogId(createdLog.getId());
    }

    public final void warn(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getBlockId(), getCurrentLogId(), LogTypeEnum.WARN, mesage, getBuffer());
        setCurrentLogId(createdLog.getId());
    }

    public final void error(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getBlockId(), getCurrentLogId(), LogTypeEnum.ERROR, mesage, getBuffer());
        setCurrentLogId(createdLog.getId());
    }

    abstract public void close(String endMessage);

    abstract protected void setCurrentLogId(String newLogId);

    abstract protected VFLBuffer getBuffer();

    abstract protected String getCurrentLogId();

    abstract protected String getBlockId();

    abstract protected void ensureBlockStarted();
}