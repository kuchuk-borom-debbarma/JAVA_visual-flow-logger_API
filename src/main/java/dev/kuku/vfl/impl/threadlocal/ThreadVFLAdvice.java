package dev.kuku.vfl.impl.threadlocal;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class ThreadVFLAdvice {
    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method,
                        @Advice.AllArguments Object[] args) {
        String first = args.length > 0 ? String.valueOf(args[0]) : "";
        System.out.println(method.getName() + "(" + first + ") started");
    }

    @Advice.OnMethodExit
    static void onExit() {
        System.out.println("ended");
    }
}
