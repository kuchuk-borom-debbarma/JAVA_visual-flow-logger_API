package dev.kuku.vfl.impl.annotation;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * Visual Flow Logger (VFL) initializer for annotation-based tracing.
 *
 * <p>This class sets up runtime bytecode instrumentation to automatically inject
 * custom VFL tracing logic into methods annotated with {@link SubBlock}.
 * It uses the ByteBuddy Java agent to modify method bytecode at load time.
 *
 * <p><b>Important notes:</b>
 * <ul>
 *   <li>Call {@link #initialize(VFLAnnotationConfig)} <b>very early</b> — ideally before any application
 *       classes containing {@code @SubBlock} methods are loaded — to ensure those methods get instrumented.</li>
 *   <li>Static methods in the same class that calls this initializer cannot be instrumented
 *       because their declaring class will already be loaded.</li>
 *   <li>If {@code disabled} is true, this initializer exits immediately without setting up anything.</li>
 * </ul>
 */
public class VFLInitializer {

    static Logger log = LoggerFactory.getLogger(VFLInitializer.class);
    static VFLAnnotationConfig VFLAnnotationConfig;
    static volatile boolean initialized = false;

    /**
     * Checks if VFL annotation-based tracing is currently disabled.
     *
     * @return {@code true} if disabled or not initialized, otherwise {@code false}.
     */
    public static boolean isDisabled() {
        return !initialized || VFLAnnotationConfig == null || VFLAnnotationConfig.disabled;
    }

    /**
     * Initializes VFL annotation-based instrumentation.
     *
     * <p>This will:
     * <ul>
     *   <li>Install the ByteBuddy agent into the running JVM</li>
     *   <li>Store the given {@link VFLAnnotationConfig}</li>
     *   <li>Instrument all non-abstract methods annotated with {@link SubBlock} so that VFL tracing code
     *       is automatically executed at their start and end.</li>
     * </ul>
     *
     * <p><b>Parameters:</b>
     * <ul>
     *     <li><b>{@code disabled}</b> — if true, skips all instrumentation and disables tracing/logging globally.</li>
     *     <li><b>{@code buffer}</b> — the {@link dev.kuku.vfl.core.buffer.VFLBuffer} where logs are stored before being flushed.</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * VFLAnnotationConfig config =
     *      new VFLAnnotationConfig(false, new MyCustomBuffer());
     *
     * VFLInitializer.initialize(config);
     * }</pre>
     *
     * <p><b>Caution:</b>
     * This must be called before annotated classes are loaded.
     * Once a class is loaded, its bytecode cannot be modified unless retransformation is explicitly allowed.
     *
     * @param config VFL annotation configuration with buffer and enable/disable flag
     */
    public static synchronized void initialize(VFLAnnotationConfig config) {
        if (config == null || config.disabled) {
            return; // Do nothing if config is missing or disabled
        }
        try {
            // Attach ByteBuddy agent to JVM
            Instrumentation inst = ByteBuddyAgent.install();
            VFLInitializer.VFLAnnotationConfig = config;

            // Configure ByteBuddy to transform classes with @SubBlock annotated methods
            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(new AgentBuilder.Listener() {
                        @Override
                        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                                     JavaModule module, boolean loaded, DynamicType dynamicType) {
                            log.debug("[VFL] Successfully transformed: {}", typeDescription.getName());
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader,
                                              JavaModule module, boolean loaded) {}

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module,
                                            boolean loaded, Throwable throwable) {
                            log.error("[VFL] Error transforming: {}", typeName, throwable);
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader,
                                               JavaModule module, boolean loaded) {}
                    })
                    // Match classes that declare any method annotated with @SubBlock
                    .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(SubBlock.class)))
                    // Inject advice into those annotated methods, excluding abstract ones
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                        log.debug("[VFL] Attempting to instrument: {}", typeDescription.getName());
                        return builder.visit(
                                Advice.to(VFLAnnotationAdvice.class)
                                        .on(ElementMatchers.isAnnotatedWith(SubBlock.class)
                                                .and(ElementMatchers.not(ElementMatchers.isAbstract())))
                        );
                    })
                    .installOn(inst);

            initialized = true;
            log.info("[VFL] Instrumentation initialised successfully");
        } catch (Exception e) {
            log.error("[VFL] Initialisation failed", e);
            throw new RuntimeException(e);
        }
    }
}
