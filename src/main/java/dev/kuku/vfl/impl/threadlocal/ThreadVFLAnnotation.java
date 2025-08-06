package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

import static dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotationAdvice.parentThreadLoggerData;

/**
 * Annotation support for methods using @Advice.FieldValue for field injection.
 * The only limitation is it is not possible to annotate static methods in the same class
 * where it's getting initialized. Also, make sure to initialize as early as possible.
 */
public class ThreadVFLAnnotation {
    private static final Logger log = LoggerFactory.getLogger(ThreadVFLAnnotation.class);
    static volatile boolean initialized = false;

    @Advice.OnMethodEnter
    public static synchronized void initialise(VFLBuffer buffer, boolean disable) {
        if (disable) {
            log.debug("Disable flag is true, skipping instrumentation");
            return;
        }
        if (initialized) {
            log.debug("[VFL] Agent already initialised – skipping");
            return;
        }

        try {
            Instrumentation inst = ByteBuddyAgent.install();
            ThreadVFLAnnotationAdvice.buffer = buffer;

            // Install advice using @Advice.FieldValue for field injection
            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(VFLBlock.class))
                            .and(ElementMatchers.not(ElementMatchers.is(ThreadVFLAnnotation.class))))
                    .transform((b, td, cl, jm, pd) ->
                            b.visit(Advice.to(ThreadVFLAnnotationAdvice.class)
                                    .on(ElementMatchers.isAnnotatedWith(VFLBlock.class)))
                    )
                    .installOn(inst);

            initialized = true;
            log.debug("[VFL] Instrumentation initialised successfully");
        } catch (Exception e) {
            log.error("[VFL] Initialisation failed", e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized void reset() {
        initialized = false;
        ThreadVFLAnnotationAdvice.buffer = null;
        if (parentThreadLoggerData.get() != null) {
            parentThreadLoggerData.remove();
        }
    }
}