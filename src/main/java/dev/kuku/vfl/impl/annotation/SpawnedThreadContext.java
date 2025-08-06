package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

public record SpawnedThreadContext(VFLBlockContext parentContext, LogTypeBlockStartEnum startType) {
}
