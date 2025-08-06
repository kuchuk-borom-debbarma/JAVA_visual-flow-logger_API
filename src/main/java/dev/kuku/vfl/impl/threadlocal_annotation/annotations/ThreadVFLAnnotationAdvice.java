package dev.kuku.vfl.impl.threadlocal_annotation.annotations;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal_annotation.logger.ThreadVFL;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Stack;

import static dev.kuku.vfl.core.helpers.Util.GetMethodName;
import static dev.kuku.vfl.impl.threadlocal_annotation.annotations.ThreadVFLAdviceData.buffer;
import static dev.kuku.vfl.impl.threadlocal_annotation.annotations.ThreadVFLAdviceData.parentThreadLoggerData;

public class ThreadVFLAnnotationAdvice {
    public static Logger log = LoggerFactory.getLogger(ThreadVFLAnnotationAdvice.class);

    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args) {
        if (!ThreadVFLAnnotationProcessor.initialized) return;

        String blockName = GetMethodName(method, args);
        // 1. No stack yet – either root call or sub-block in fresh thread
        if (ThreadVFL.LOGGER_STACK.get() == null) {
            if (parentThreadLoggerData.get() == null) {
                /* ---------- ROOT BLOCK ---------- */
                log.debug("[VFL] ROOT-start {}", blockName);

                Block rootBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, null, buffer);
                ThreadVFL logger = new ThreadVFL(new VFLBlockContext(rootBlock, buffer));

                ThreadVFL.LOGGER_STACK.set(new Stack<>());
                ThreadVFL.LOGGER_STACK.get().push(logger);
                logger.ensureBlockStarted();
                return;
            }

            /* ---------- NEW THREAD SUB-BLOCK ---------- */
            log.debug("[VFL] THREAD-inherit {} from parent {}", blockName, parentThreadLoggerData.get().parentContext().blockInfo.getBlockName());
            VFLBlockContext parentContext = parentThreadLoggerData.get().parentContext();
            LogTypeBlockStartEnum startType = parentThreadLoggerData.get().startType();

            //Create sub block
            Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentContext.blockInfo.getId(), buffer);
            //Start sub block in parent  when it's called
            SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(parentContext.blockInfo.getId(),
                    parentContext.currentLogId, null, subBlock.getId(), startType, buffer);
            //Create sub block context and logger
            VFLBlockContext subBlockLoggerContext = new VFLBlockContext(subBlock, buffer);
            ThreadVFL logger = new ThreadVFL(subBlockLoggerContext);

            //Push the logger to this newly created thread local logger stack
            ThreadVFL.LOGGER_STACK.set(new Stack<>());
            ThreadVFL.LOGGER_STACK.get().push(logger);

            logger.ensureBlockStarted();
            return;
        }

        /* ---------- REGULAR SUB-BLOCK ---------- */
        VFLBlockContext parentCtx = Objects.requireNonNull(ThreadVFL.getCurrentLogger()).loggerContext;
        log.debug("[VFL] SUB-start {} → {}", parentCtx.blockInfo.getBlockName(), blockName);

        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentCtx.blockInfo.getId(), buffer);

        SubBlockStartLog sLog = VFLFlowHelper.CreateLogAndPush2Buffer(parentCtx.blockInfo.getId(), parentCtx.currentLogId, null, subBlock.getId(), LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY, buffer);

        parentCtx.currentLogId = sLog.getId();

        ThreadVFL subLogger = new ThreadVFL(new VFLBlockContext(subBlock, buffer));
        ThreadVFL.LOGGER_STACK.get().push(subLogger);
        subLogger.ensureBlockStarted();
    }

    @Advice.OnMethodExit
    static void onExit(@Advice.Origin Method method,
                       @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnedValue
    ) {
        if (!ThreadVFLAnnotationProcessor.initialized) return;
        // close current logger
        ThreadVFL logger = ThreadVFL.getCurrentLogger();
        log.debug("[VFL] EXIT {} (blockId={})", method.getName(), Objects.requireNonNull(logger).loggerContext.blockInfo.getId());
        logger.onClose(returnedValue == null ? null : "Returned " + returnedValue);
        //Check if it was root block, if yes then flush buffer
        if (ThreadVFL.LOGGER_STACK.get() == null) {
            if (parentThreadLoggerData.get() == null) {
                log.debug("[VFL] ROOT complete – flushing buffer");
                buffer.flushAndClose();
            }
            log.debug("Removing parentThreadLoggerDataReflected from {} If valid.", Util.GetThreadInfo());
            parentThreadLoggerData.remove();
        }
    }
}
