package dev.kuku.vfl.core.helpers;

import dev.kuku.vfl.core.abstracts.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.helpers.Util.UID;

public class VFLHelper {

    public static Log CreateLogAndPush2Buffer(String blockId, String parentLogId, LogTypeEnum logType, String message, VFLBuffer buffer) {
        Log l = new Log(UID(), blockId, parentLogId, logType, message, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(l);
        return l;
    }

    public static SubBlockStartLog CreateLogAndPush2Buffer(String blockId, String parentLogId, String startMessage, String referencedBlockId, LogTypeBlcokStartEnum logType, VFLBuffer buffer) {
        SubBlockStartLog l = new SubBlockStartLog(UID(), blockId, parentLogId, startMessage, referencedBlockId, logType);
        buffer.pushLogToBuffer(l);
        return l;
    }

    public static <R> R CallFnWithLogger(Callable<R> callable, VFL logger, Function<R, String> endMessageSerializer) {
        R result = null;
        try {
            result = callable.call();
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            String endMsg = null;
            if (endMessageSerializer != null) {
                try {
                    endMsg = endMessageSerializer.apply(result);
                } catch (Exception e) {
                    endMsg = "Failed to serialzie end message " + e.getMessage();
                }
            }
            logger.close(endMsg);
        }
        return result;
    }
}