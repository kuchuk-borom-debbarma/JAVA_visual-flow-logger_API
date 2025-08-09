package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Configuration {
    static Configuration INSTANCE;
    final boolean disabled;
    final VFLBuffer buffer;
    final boolean autoCreateRootBlock;
}
