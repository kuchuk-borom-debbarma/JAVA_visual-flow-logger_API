package dev.kuku.vfl;

import dev.kuku.vfl.core.models.*;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

/**
 * Helper class containing functions that are core common logic shared amongst all logger with block logging functionality. <br>
 * Used by :- <br>
 * {@link dev.kuku.vfl.PassthroughVFL} <br>
 * {@link dev.kuku.vfl.ThreadVFL} <br>
 * {@link dev.kuku.vfl.ScopedVFL} <br>
 * <br>
 */
class BlockHelper {
    private BlockHelper() {
    }

    /**
     * Creates a block data and returns it after pushing it to buffer.
     *
     * @param id        ID to assign to the created block
     * @param blockName name to assign to the block
     * @param ctx       (Optional) context of current logger if intending to create a sub block data.
     */
    public static BlockData CreateBlockDataAndPush(String id, String blockName, VFLBlockContext ctx) {
        String parentId = null;
        try {
            parentId = ctx.blockInfo.getId();
        } catch (NullPointerException _) {

        }
        var b = new BlockData(id, parentId, blockName);
        ctx.buffer.pushBlockToBuffer(b);
        return b;
    }

    /**
     * Performs the steps required for setting up a Log of type SubBlockStart before the provided method is invoked. <br>
     * Should be used for starting a sub block call/run. <br> <br>
     * Example :- <br>
     * used in {@link dev.kuku.vfl.PassthroughVFL#run(String, String, Consumer)} to setup new block and log before executing the passed consumer.
     *
     * @param blockName      Name to give to the created sub block data
     * @param startMessage   Message to assign to the created log of type {@link VflLogType#SUB_BLOCK_START}
     * @param moveFwd        Whether to move forward the log chain or stay in current position
     * @param blockContext   Context of the logger that is starting the the sub block call/run
     * @param createLoggerFn Function that needs to take in the {@link VFLBlockContext} and return a {@link VFL} instance.
     * @param afterSetupFn   Optional method to invoke after setup is complete before returning the setup result. One of it's use is in {@link dev.kuku.vfl.ScopedVFL#run(String, String, Runnable)} to run the passed runnable parameter after setup.
     * @return Created subLogger, block data, log data.
     */
    public static LoggerAndBlockLogData SetupSubBlockStart(String blockName, String startMessage, boolean moveFwd, VFLBlockContext blockContext, Function<VFLBlockContext, VFL> createLoggerFn, Consumer<LoggerAndBlockLogData> afterSetupFn) {
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

    /**
     * Calls the provided function and uses the provided logger to log exception(if any) and to close the logger once callable has finished invoking. <br>
     * It is meant to be used for invoking block functions with the block's logger provided as the logger. <br>
     * <p>
     * One of it's use-case is in {@link dev.kuku.vfl.ThreadVFL#call(String, String, Callable, Function)}
     *
     * @param callable The method to call
     * @param endMsgFn A method that takes in the result of the callable's invokation return value and returns a string. This string will be set as the end message for the closing block log {@link VflLogType#BLOCK_END}.
     * @param onError  method to invoke upon error. Takes in the exception as input.
     * @param logger   The logger that needs to be closed and log exception(if thrown).
     * @return value returned after invoking callable
     */
    public static <R> R CallFnForLogger(Callable<R> callable, Function<R, String> endMsgFn, Consumer<Exception> onError, IVFL logger) {
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
     * Please look at {@link BlockHelper#CallFnForLogger(Callable, Function, Consumer, IVFL)}.
     */
    public static <R> void RunFnForLogger(Runnable runnable, Consumer<Exception> onError, IVFL logger) {
        BlockHelper.CallFnForLogger(() -> {
            runnable.run();
            return null;
        }, null, onError, logger);
    }
}