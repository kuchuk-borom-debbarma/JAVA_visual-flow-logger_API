package dev.kuku.vfl.impl.threadlocal;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class VFLInstrumentation {
    public static void install() {
        ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .type(ElementMatchers.any())
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.isAnnotatedWith(VFLBlock.class))
                                .intercept(Advice.to(ThreadVFLAdvice.class)))
                .installOnByteBuddyAgent();
    }

}