package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            getContext().buffer.pushBlockToBuffer(getContext().blockInfo);
        }
    }

    public void close(String endMessage) {
        ensureBlockStarted();
        getContext().buffer.pushLogEndToBuffer(getContext().blockInfo.getId(), endMessage);
    }

    public final void log(String mesage) {
        if (!getContext().allowedLogTypes.contains(LogTypeEnum.MESSAGE.toString())) {
            return;
        }
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, LogTypeEnum.MESSAGE, mesage, getContext().buffer);
        getContext().currentLogId = createdLog.getId();
    }

    public final void warn(String mesage) {
        if (!getContext().allowedLogTypes.contains(LogTypeEnum.WARN.toString())) {
            return;
        }
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, LogTypeEnum.MESSAGE, mesage, getContext().buffer);
        getContext().currentLogId = createdLog.getId();
    }

    public final void error(String mesage) {
        if (!getContext().allowedLogTypes.contains(LogTypeEnum.ERROR.toString())) {
            return;
        }
        ensureBlockStarted();
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, LogTypeEnum.MESSAGE, mesage, getContext().buffer);
        getContext().currentLogId = createdLog.getId();
    }

    abstract protected VFLBlockContext getContext();

}