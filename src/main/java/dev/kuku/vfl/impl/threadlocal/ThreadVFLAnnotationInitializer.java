package dev.kuku.vfl.impl.threadlocal;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class ThreadVFLAnnotationInitializer {
    private static boolean setUp = false;

    public static synchronized void initialise() {
        if (setUp) return;

        ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .type(ElementMatchers.any())
                .transform((builder, td, cl, m, pd) ->
                        builder.visit(Advice.to(ThreadVFLAdvice.class)
                                .on(ElementMatchers.isAnnotatedWith(VFLBlock.class))))
                .installOn(ByteBuddyAgent.getInstrumentation());

        setUp = true;
    }
}
