package dev.kuku.vfl.impl.threadlocal.annotations;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

public record SpawnedThreadContext(VFLBlockContext parentContext, LogTypeBlockStartEnum startType) {
}
