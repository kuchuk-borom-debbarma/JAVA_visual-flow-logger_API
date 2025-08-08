package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

public record SpawnedThreadContext(BlockContext parentContext, LogTypeBlockStartEnum startType) {
}
