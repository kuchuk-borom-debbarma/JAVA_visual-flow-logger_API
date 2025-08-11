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

public class VFLInitializer {
    static Logger log = LoggerFactory.getLogger(VFLInitializer.class);
    static VFLAnnotationConfig VFLAnnotationConfig;
    static volatile boolean initialized = false;

    /**
     * Helper method to determine if VFL is functional or not.
     *
     * @return true if disabled else false
     */
    public static boolean isDisabled() {
        return !initialized || VFLAnnotationConfig == null || VFLAnnotationConfig.disabled;
    }

    /**
     * Initialize Visual Flow Logger, this will inject code on methods annotated with @SubBlock at start and end.
     * Make sure to ALWAYS do this first before the classes are loaded in JVM.
     * Annotation on static methods will not work IF it's in the same class where the VFL initializer is invoked. This is because the class and the static methods are loaded first and thus cant have custom code injected in them.
     *
     * @param config configuration
     */
    public static synchronized void initialize(VFLAnnotationConfig config) {
        if (config == null || config.disabled) {
            return;
        }
        try {
            Instrumentation inst = ByteBuddyAgent.install();
            VFLInitializer.VFLAnnotationConfig = config;

            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(new AgentBuilder.Listener() {
                        @Override
                        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                            // Called when a type is discovered for transformation
                        }

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                                     JavaModule module, boolean loaded, DynamicType dynamicType) {
                            log.debug("[VFL] Successfully transformed: {}", typeDescription.getName());
                        }

                        @Override
                        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader,
                                              JavaModule module, boolean loaded) {
                            // Called when a type is ignored
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module,
                                            boolean loaded, Throwable throwable) {
                            log.error("[VFL] Error transforming: {}", typeName, throwable);
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                            // Called when transformation is complete
                        }
                    })
                    // Target classes that declare methods with @SubBlock annotation
                    .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(SubBlock.class)))
                    .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                        log.debug("[VFL] Attempting to instrument: {}", typeDescription.getName());
                        return builder.visit(
                                Advice.to(VFLAnnotationAdvice.class)
                                        .on(ElementMatchers.isAnnotatedWith(SubBlock.class)
                                                .and(ElementMatchers.not(ElementMatchers.isAbstract())) // Exclude abstract methods
                                        )
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