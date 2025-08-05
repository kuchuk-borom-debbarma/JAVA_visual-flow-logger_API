package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Stack;

import static dev.kuku.vfl.core.helpers.Util.getMethodName;

public class ThreadVFLAnnotation {
    public static final ThreadLocal<Block> startedSubBlockInParentThread = new ThreadLocal<>();
    public static Logger log = LoggerFactory.getLogger(ThreadVFLAnnotation.class);
    public static VFLBuffer buffer;
    public static volatile boolean initialized = false;

    /**
     * Entry-point that installs the Byte Buddy agent exactly once.
     */
    public static synchronized void initialise(VFLBuffer buffer) {
        if (initialized) {
            log.debug("[VFL] Agent already initialised – skipping");
            return;
        }

        try {
            Instrumentation inst = ByteBuddyAgent.install();
            ThreadVFLAnnotation.buffer = buffer;

            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(VFLBlock.class)))
                    .transform((b, td, cl, jm, pd) ->
                            b.visit(Advice.to(ThreadVFLAnnotation.class)
                                    .on(ElementMatchers.isAnnotatedWith(VFLBlock.class))))
                    .installOn(inst);

            initialized = true;
            log.debug("[VFL] Instrumentation initialised successfully");
        } catch (Exception e) {
            log.error("[VFL] Initialisation failed", e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized void reset() {
        initialized = false;
        buffer = null;
    }

    /* ---------------- Advice logic ---------------- */

    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method,
                        @Advice.AllArguments Object[] args) {

        String blockName = getMethodName(method, args);

        // 1. No stack yet – either root call or sub-block in fresh thread
        if (ThreadVFL.LOGGER_STACK.get() == null) {

            if (startedSubBlockInParentThread.get() == null) {
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
            log.debug("[VFL] THREAD-inherit {}", blockName);

            ThreadVFL.LOGGER_STACK.set(new Stack<>());
            ThreadVFL logger = new ThreadVFL(
                    new VFLBlockContext(startedSubBlockInParentThread.get(), buffer));

            ThreadVFL.LOGGER_STACK.get().push(logger);
            logger.ensureBlockStarted();
            return;
        }

        /* ---------- REGULAR SUB-BLOCK ---------- */
        VFLBlockContext parentCtx = ThreadVFL.getCurrentLogger().loggerContext;
        log.debug("[VFL] SUB-start {} → {}", parentCtx.blockInfo.getBlockName(), blockName);

        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName, parentCtx.blockInfo.getId(), buffer);

        SubBlockStartLog sLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                parentCtx.blockInfo.getId(),
                parentCtx.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                buffer);

        parentCtx.currentLogId = sLog.getId();

        ThreadVFL subLogger = new ThreadVFL(new VFLBlockContext(subBlock, buffer));
        ThreadVFL.LOGGER_STACK.get().push(subLogger);
        subLogger.ensureBlockStarted();
    }

    @Advice.OnMethodExit
    static void onExit(@Advice.Origin Method method) {

        ThreadVFL logger = ThreadVFL.getCurrentLogger();
        log.debug("[VFL] EXIT {} (blockId={})",
                method.getName(),
                logger.loggerContext.blockInfo.getId());

        logger.onClose(null);

        if (ThreadVFL.LOGGER_STACK.get() == null) {          // stack emptied
            if (startedSubBlockInParentThread.get() == null) {
                log.debug("[VFL] ROOT complete – flushing buffer");
                buffer.flushAndClose();
            }
            startedSubBlockInParentThread.remove();
        }
    }
}
