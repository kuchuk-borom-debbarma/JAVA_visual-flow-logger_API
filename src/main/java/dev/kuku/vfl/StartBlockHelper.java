package dev.kuku.vfl;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.models.*;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class StartBlockHelper {
    /**
     * calls the provided function, on exception logs the error, runs the passed onError fn and re-throws exception. <br>
     * Upon completion closes the block after processing endMsgFn.
     */
    public static <R> R ProcessCallableForLogger(Callable<R> callable, Function<R, String> endMsgFn, Consumer<Exception> onError, IVFL logger) {
        R result = null;
        try {
            result = callable.call();
        } catch (Exception e) {
            logger.error(String.format(e.getClass().getSimpleName() + " " + e.getMessage()));
            if (onError != null) {
                onError.accept(e);
            }
            throw new RuntimeException(e);
        } finally {
            String endMsg = null;
            if (endMsgFn != null) {
                try {
                    endMsg = endMsgFn.apply(result);
                } catch (Exception e) {
                    endMsg = String.format("Failed processing end message : " + e.getClass().getSimpleName() + " " + e, e.getMessage());
                }
            }
            logger.closeBlock(endMsg);
        }
        return result;
    }

    /**
     * Same as {@link StartBlockHelper#ProcessCallableForLogger(Callable, Function, Consumer, IVFL)} but doesn't return a valud and has no endMessage.
     */
    public static <R> void RunFnForLogger(Runnable runnable, Consumer<Exception> onError, IVFL logger) {
        StartBlockHelper.ProcessCallableForLogger(() -> {
            runnable.run();
            return null;
        }, null, onError, logger);
    }

    public static LoggerAndBlockLogData SetupStartBlock(String blockName, String startMessage, boolean moveFwd, VFLBlockContext blockContext, Function<VFLBlockContext, VFL> createLoggerFn, Consumer<LoggerAndBlockLogData> afterSetupFn) {
        String subBlockId = generateUID();
        BlockData blockData = new BlockData(subBlockId, blockContext.blockInfo.getId(), blockName);
        LogData logData = new LogData(generateUID(), blockContext.blockInfo.getId(), blockContext.currentLogId, VflLogType.SUB_BLOCK_START, startMessage, subBlockId, Instant.now().toEpochMilli());
        VFLBlockContext subBlockCtx = new VFLBlockContext(blockData, blockContext.buffer);
        blockContext.buffer.pushBlockToBuffer(blockData);
        blockContext.buffer.pushLogToBuffer(logData);
        if (moveFwd) {
            blockContext.currentLogId = logData.getId();
        }
        var logger = createLoggerFn.apply(subBlockCtx);
        LoggerAndBlockLogData createdData = new LoggerAndBlockLogData(logger, blockData, logData);
        if (afterSetupFn != null)
            afterSetupFn.accept(createdData);
        return createdData;
    }

    public static BlockData CreateBlockDataAndPush(String id, String blockName, VFLBlockContext ctx) {
        var b = new BlockData(id, ctx.blockInfo.getId(), blockName);
        ctx.buffer.pushBlockToBuffer(b);
        return b;
    }

}