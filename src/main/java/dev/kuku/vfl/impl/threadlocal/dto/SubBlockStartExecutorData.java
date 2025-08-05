package dev.kuku.vfl.impl.threadlocal.dto;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

public record SubBlockStartExecutorData(VFLBlockContext parentContext, LogTypeBlockStartEnum startType) {
}
