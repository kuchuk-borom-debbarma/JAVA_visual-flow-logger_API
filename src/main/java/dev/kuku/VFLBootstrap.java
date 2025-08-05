package dev.kuku;

import dev.kuku.vfl.impl.threadlocal.ThreadVFLAdvice;
import dev.kuku.vfl.impl.threadlocal.VFLBlock;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class VFLBootstrap {
    public static void main(String[] args) {
        // Install instrumentation first
        ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .type(ElementMatchers.nameStartsWith("dev.kuku")) // Target your package
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.isAnnotatedWith(VFLBlock.class))
                                .intercept(Advice.to(ThreadVFLAdvice.class)))
                .installOnByteBuddyAgent();

        // Now call your main class
        Main.main(args);
    }
}
