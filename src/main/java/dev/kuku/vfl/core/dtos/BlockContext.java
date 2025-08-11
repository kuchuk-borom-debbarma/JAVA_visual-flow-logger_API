package dev.kuku.vfl.core.dtos;

import dev.kuku.vfl.core.models.Block;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@ToString
public class BlockContext {
    public final Block blockInfo;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public String currentLogId;
}
