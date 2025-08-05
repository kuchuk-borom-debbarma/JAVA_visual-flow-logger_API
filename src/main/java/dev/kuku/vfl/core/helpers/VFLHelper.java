package dev.kuku.vfl.core.helpers;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.UID;

public class VFLHelper {

    public static Log CreateLogAndPush2Buffer(String blockId, String parentLogId, LogTypeEnum logType, String message, VFLBuffer buffer) {
        Log l = new Log(UID(), blockId, parentLogId, logType, message, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(l);
        return l;
    }

    public static SubBlockStartLog CreateLogAndPush2Buffer(String blockId, String parentLogId, String startMessage, String referencedBlockId, LogTypeBlockStartEnum logType, VFLBuffer buffer) {
        SubBlockStartLog l = new SubBlockStartLog(UID(), blockId, parentLogId, startMessage, referencedBlockId, logType);
        buffer.pushLogToBuffer(l);
        return l;
    }

    public static Block CreateBlockAndPush2Buffer(String blockName, String parentBlockId, VFLBuffer buffer) {
        Block b = new Block(UID(), parentBlockId, blockName);
        buffer.pushBlockToBuffer(b);
        return b;
    }

    public static <R> R CallFnWithLogger(Supplier<R> supplier, VFL logger, Function<R, String> endMessageSerializer) {
        R result = null;
        try {
            result = supplier.get();
            return result;
        } catch (Exception e) {
            logger.error("Exception occurred: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally {
            String endMsg = createEndMessage(result, endMessageSerializer);
            logger.close(endMsg);
        }
    }

    private static <R> String createEndMessage(R result, Function<R, String> serializer) {
        if (serializer == null) return null;

        try {
            return serializer.apply(result);
        } catch (Exception e) {
            return "Failed to serialize end message: " + e.getMessage();
        }
    }


}