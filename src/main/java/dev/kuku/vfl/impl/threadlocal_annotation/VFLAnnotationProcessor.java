package dev.kuku.vfl.impl.threadlocal_annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.impl.threadlocal_annotation.annotations.ThreadVFLAdviceData;
import dev.kuku.vfl.impl.threadlocal_annotation.annotations.ThreadVFLAnnotationProcessor;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class VFLAnnotationProcessor {
    public static Logger log = LoggerFactory.getLogger(ThreadVFLAnnotationProcessor.class);

    public static volatile boolean initialized = false;

    public static synchronized void initialise(VFLBuffer buffer) {
        try {
            Instrumentation inst = ByteBuddyAgent.install();
            ThreadVFLAdviceData.buffer = buffer;

            new AgentBuilder.Default().with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(VFLBlock.class)))
                    .transform((b, td, cl, jm, pd) ->
                            b.visit(Advice.to(VFLAnnotationAdvice.class)
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
        ThreadVFLAdviceData.buffer = null;
    }
}
