package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VFLAnnotationConfig {
    public final boolean disabled;
    public final VFLBuffer buffer;
}
