package dev.kuku.vfl.impl.threadlocal_annotation;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class VFLAnnotationAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        //TODO start block
    }

    @Advice.OnMethodExit
    public static void onExit() {
        //TODO end block
    }
}
