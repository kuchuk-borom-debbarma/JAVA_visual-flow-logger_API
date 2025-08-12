package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import lombok.RequiredArgsConstructor;

/**
 * Configuration object for initializing Visual Flow Logger (VFL) annotation-based tracing.
 *
 * <p>This config is passed to {@link VFLInitializer#initialize(VFLAnnotationConfig)} to control
 * whether VFL is enabled and where trace data is stored.
 *
 * <p><b>Fields:</b>
 * <ul>
 *   <li>{@code disabled} – If {@code true}, VFL is completely disabled:
 *       <ul>
 *           <li>No bytecode instrumentation will be applied to annotated methods</li>
 *           <li>{@link Log} calls will be ignored</li>
 *           <li>This can be used as a quick global "off switch" for VFL</li>
 *       </ul>
 *   </li>
 *   <li>{@code buffer} – The {@link VFLBuffer} implementation used to store
 *       logs and block execution data before flushing.
 *       This can be an in-memory, async, or custom implementation depending on your needs.</li>
 * </ul>
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * VFLBuffer buffer = new MyCustomBuffer();
 * VFLAnnotationConfig config = new VFLAnnotationConfig(false, buffer);
 * VFLInitializer.initialize(config);
 * }</pre>
 */
@RequiredArgsConstructor
public class VFLAnnotationConfig {

    /**
     * If {@code true}, disables all VFL functionality.
     * No instrumentation will be applied and {@link Log} calls will not output anything.
     */
    public final boolean disabled;

    /**
     * The buffer used for collecting and temporarily storing VFL logs and flow data.
     * Must not be {@code null} when {@code disabled} is false.
     */
    public final VFLBuffer buffer;
}
