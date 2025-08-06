package dev.kuku.vfl.impl.threadlocal_annotation;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class VFLAnnotationAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
    }

    @Advice.OnMethodExit
    public static void onExit() {

    }
}
