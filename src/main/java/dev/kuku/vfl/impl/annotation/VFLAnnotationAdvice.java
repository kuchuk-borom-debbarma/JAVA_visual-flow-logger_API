package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

import static dev.kuku.vfl.core.helpers.Util.GetMethodName;

@Slf4j
public class VFLAnnotationAdvice {
    public static final VFLAnnotationAdvice instance = new VFLAnnotationAdvice();

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        VFLAnnotationAdvice.instance.on_enter(method, args);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
                              @Advice.AllArguments Object[] args,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
                              @Advice.Thrown Throwable threw) {
        VFLAnnotationAdvice.instance.on_exit(method, args, returnedValue, threw);
    }

    public void on_enter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        String blockName = GetMethodName(method, args);
        log.debug("Entered SubBlock: {}", blockName);

        BlockContext parentBlockContext;

        //Parent already exists
        if (ThreadContextManager.GetCurrentBlockContext() != null) {
            parentBlockContext = ThreadContextManager.GetCurrentBlockContext();
        }
        //Can't create sub block for annotated method.
        else {
            log.warn("Could not create block for @SubBlock-{}: no parent block", blockName);
            return;
        }
        //This should never happen but you never know
        if (parentBlockContext == null) {
            log.error("Parent Block Context is null when it should not be. It should have been valid after assigning it from top most stack or from spawnedThreadContext.");
            return;
        }

        log.debug(
                "Creating sub-block '{}' from parent '{}-{}'.",
                blockName,
                parentBlockContext.blockInfo.getBlockName(),
                Util.TrimId(parentBlockContext.blockInfo.getId()));

        //Create sub block
        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentBlockContext.blockInfo.getId(), VFLInitializer.VFLAnnotationConfig.buffer);
        //Create Sub block start log for parent
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(parentBlockContext.blockInfo.getId(),
                parentBlockContext.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                VFLInitializer.VFLAnnotationConfig.buffer);
        ThreadContextManager.GetCurrentBlockContext().currentLogId = subBlockStartLog.getId();
        ThreadContextManager.PushBlockToThreadLogStack(subBlock);
    }

    public void on_exit(@Advice.Origin Method method,
                        @Advice.AllArguments Object[] args,
                        @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
                        @Advice.Thrown Throwable threw) {
        String blockName = GetMethodName(method, args);

        if (threw != null) {
            Log.Error("Exception: {}-{}", threw.getClass().getName(), threw.getMessage());
        }

        String endMsg = "Returned : " + returnedValue;

        ThreadContextManager.CloseAndPopCurrentContext(endMsg);
    }
}