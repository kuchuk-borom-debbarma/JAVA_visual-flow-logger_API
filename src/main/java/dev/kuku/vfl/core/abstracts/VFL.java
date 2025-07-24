package dev.kuku.vfl.core.abstracts;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

public abstract class VFL {
    public final void log(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getCurrentLogId(), getParentLogId(), LogTypeEnum.MESSAGE, mesage, getBuffer());
        updateLogFlow(createdLog.getId());
    }

    public final void warn(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getCurrentLogId(), getParentLogId(), LogTypeEnum.WARN, mesage, getBuffer());
        updateLogFlow(createdLog.getId());
    }

    public final void error(String mesage) {
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getCurrentLogId(), getParentLogId(), LogTypeEnum.ERROR, mesage, getBuffer());
        updateLogFlow(createdLog.getId());
    }

    abstract protected void updateLogFlow(String newLogId);

    abstract protected VFLBuffer getBuffer();

    abstract protected String getParentLogId();

    abstract protected String getCurrentLogId();

    abstract protected void ensureBlockStarted();
}