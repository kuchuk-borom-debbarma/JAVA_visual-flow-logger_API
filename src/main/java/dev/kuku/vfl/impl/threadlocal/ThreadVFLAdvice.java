package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class ThreadVFLAdvice {
    static VFLBuffer buffer;

    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method,
                        @Advice.AllArguments Object[] args) {
        String first = args.length > 0 ? String.valueOf(args[0]) : "";
        String blockName = method.getName() + "(" + first + ") started";
        System.out.println("Started " + blockName);
        System.out.println(ThreadVFL.LOGGER_STACK.get());
    }

    @Advice.OnMethodExit
    static void onExit() {
        //Pops from the thread stack as well
        System.out.println("Exited");
    }
}
