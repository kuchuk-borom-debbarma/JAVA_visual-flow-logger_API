package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
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
        log.debug("Entered SubBlock: {}", blockName);

        BlockContext parentBlockContext;

        //Parent already exists
        if (ThreadContextManager.GetCurrentBlockContext() != null) {
            parentBlockContext = ThreadContextManager.GetCurrentBlockContext();
        }
        //Can't create sub block for annotated method.
        else {
            log.warn("Could not create block for @SubBlock-{}: no parent or spawnedThreadContext, and autoCreateRootBlock is disabled.", blockName);
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
        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentBlockContext.blockInfo.getId(), Configuration.INSTANCE.buffer);
        //Create Sub block start log for parent
        VFLFlowHelper.CreateLogAndPush2Buffer(parentBlockContext.blockInfo.getId(),
                parentBlockContext.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                Configuration.INSTANCE.buffer);
        ThreadContextManager.PushBlockToThreadLogStack(subBlock);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
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