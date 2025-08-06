package dev.kuku.vfl.impl.threadlocal_annotation;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static dev.kuku.vfl.core.helpers.Util.GetMethodName;

public class VFLAnnotationAdvice {
    public static final Logger log = LoggerFactory.getLogger(VFLAnnotationAdvice.class);

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        String blockName = GetMethodName(method, args);

        if (!ContextManager.hasActiveContext()) {
            if (ContextManager.isSpawnedThread()) {
                ContextManager.startSpawnedThreadBlock(blockName);
            } else {
                ContextManager.startRootBlock(blockName);
            }
        } else {
            ContextManager.startSubBlock(blockName);
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin Method method,
                              @Advice.AllArguments Object[] args,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
                              @Advice.Thrown Throwable threw) {
        // Log exception if present
        ContextManager.logException(threw);
        // Close current context and perform cleanup
        ContextManager.closeCurrentContext(returnedValue);
    }
}