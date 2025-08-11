package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import lombok.RequiredArgsConstructor;

/**
 * Configuration for Initializing VFL.
 *
 */
@RequiredArgsConstructor
public class VFLAnnotationConfig {
    /// If true, will completely skip instrumentation and custom VFL code will not be injected on annotated methods. Use this to quickly disable VFL globally. Logging using {@link Log} will not work either
    public final boolean disabled;
    public final VFLBuffer buffer;
}
