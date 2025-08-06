package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.dto.SubBlockStartExecutorData;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Stack;

import static dev.kuku.vfl.core.helpers.Util.getMethodName;

public class ThreadVFLAnnotationAdvice {
    static final ThreadLocal<SubBlockStartExecutorData> parentThreadLoggerData = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(ThreadVFLAnnotationAdvice.class);
    static VFLBuffer buffer;

    @Advice.OnMethodEnter
    static void onEnter(
            @Advice.Origin Method method,
            @Advice.AllArguments Object[] args,
            // Inject the private static buffer field
            @Advice.FieldValue(value = "buffer", declaringType = ThreadVFLAnnotation.class)
            VFLBuffer injectedBuffer,
            // Inject the static final parentThreadLoggerData field
            @Advice.FieldValue(value = "parentThreadLoggerData", declaringType = ThreadVFLAnnotation.class)
            ThreadLocal<SubBlockStartExecutorData> injectedParentData
    ) {

        String blockName = getMethodName(method, args);

        // Validate that fields were injected correctly
        if (injectedBuffer == null) {
            log.warn("[VFL] Buffer was not injected properly for method: {}", blockName);
            return;
        }

        // 1. No stack yet – either root call or sub-block in fresh thread
        if (ThreadVFL.LOGGER_STACK.get() == null) {
            if (injectedParentData.get() == null) {
                /* ---------- ROOT BLOCK ---------- */
                log.debug("[VFL] ROOT-start {}", blockName);

                Block rootBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, null, injectedBuffer);
                ThreadVFL logger = new ThreadVFL(new VFLBlockContext(rootBlock, injectedBuffer));

                ThreadVFL.LOGGER_STACK.set(new Stack<>());
                ThreadVFL.LOGGER_STACK.get().push(logger);
                logger.ensureBlockStarted();
                return;
            }

            /* ---------- NEW THREAD SUB-BLOCK ---------- */
            log.debug("[VFL] THREAD-inherit {} from parent {}",
                    blockName, injectedParentData.get().parentContext().blockInfo.getBlockName());

            VFLBlockContext parentContext = injectedParentData.get().parentContext();
            LogTypeBlockStartEnum startType = injectedParentData.get().startType();

            // Create sub block
            Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                    blockName, parentContext.blockInfo.getId(), injectedBuffer);

            // Start sub block in parent when it's called
            SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                    parentContext.blockInfo.getId(),
                    parentContext.currentLogId,
                    null,
                    subBlock.getId(),
                    startType,
                    injectedBuffer);

            // Create sub block context and logger
            VFLBlockContext subBlockLoggerContext = new VFLBlockContext(subBlock, injectedBuffer);
            ThreadVFL logger = new ThreadVFL(subBlockLoggerContext);

            // Push the logger to this newly created thread local logger stack
            ThreadVFL.LOGGER_STACK.set(new Stack<>());
            ThreadVFL.LOGGER_STACK.get().push(logger);

            logger.ensureBlockStarted();
            return;
        }

        /* ---------- REGULAR SUB-BLOCK ---------- */
        VFLBlockContext parentCtx = Objects.requireNonNull(ThreadVFL.getCurrentLogger()).loggerContext;
        log.debug("[VFL] SUB-start {} → {}", parentCtx.blockInfo.getBlockName(), blockName);

        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName, parentCtx.blockInfo.getId(), injectedBuffer);

        SubBlockStartLog sLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                parentCtx.blockInfo.getId(),
                parentCtx.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                injectedBuffer);

        parentCtx.currentLogId = sLog.getId();

        ThreadVFL subLogger = new ThreadVFL(new VFLBlockContext(subBlock, injectedBuffer));
        ThreadVFL.LOGGER_STACK.get().push(subLogger);
        subLogger.ensureBlockStarted();
    }

    @Advice.OnMethodExit
    static void onExit(
            @Advice.Origin Method method,
            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue,
            // Inject fields needed for exit processing
            @Advice.FieldValue(value = "buffer", declaringType = ThreadVFLAnnotation.class)
            VFLBuffer injectedBuffer,
            @Advice.FieldValue(value = "parentThreadLoggerData", declaringType = ThreadVFLAnnotation.class)
            ThreadLocal<SubBlockStartExecutorData> injectedParentData
    ) {

        // Validate that fields were injected correctly
        if (injectedBuffer == null) {
            log.warn("[VFL] Buffer was not injected properly for method exit: {}", method.getName());
            return;
        }

        // Close current logger
        ThreadVFL logger = ThreadVFL.getCurrentLogger();
        if (logger == null) {
            log.warn("[VFL] No current logger found for method exit: {}", method.getName());
            return;
        }

        log.debug("[VFL] EXIT {} (blockId={})",
                method.getName(), logger.loggerContext.blockInfo.getId());

        logger.onClose(returnedValue == null ? null : "Returned " + returnedValue);

        // Check if it was root block, if yes then flush buffer
        if (ThreadVFL.LOGGER_STACK.get() == null) {
            if (injectedParentData.get() == null) {
                log.debug("[VFL] ROOT complete – flushing buffer");
                injectedBuffer.flushAndClose();
            }
            log.debug("Removing parentThreadLoggerData from {} if valid.", Util.getThreadInfo());
            injectedParentData.remove();
        }
    }

}
