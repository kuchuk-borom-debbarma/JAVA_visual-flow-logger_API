package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
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
    public static Logger log = LoggerFactory.getLogger(VFLInitializer.class);

    public static volatile boolean initialized = false;

    public static synchronized void initialise(VFLBuffer buffer) {
        try {
            Instrumentation inst = ByteBuddyAgent.install();
            ThreadContextManager.AnnotationBuffer = buffer;

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

            // Log some debug information
            log.debug("[VFL] VFLBuffer set: {}", ThreadContextManager.AnnotationBuffer != null);

        } catch (Exception e) {
            log.error("[VFL] Initialisation failed", e);
            throw new RuntimeException(e);
        }
    }
}