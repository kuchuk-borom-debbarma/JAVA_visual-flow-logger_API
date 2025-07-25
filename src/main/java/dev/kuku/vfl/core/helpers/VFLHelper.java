package dev.kuku.vfl.core.helpers;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

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

    public static Set<String> GetLogsAsStringSet(Set<LogTypeEnum> typeToRemove, Set<LogTypeBlockStartEnum> startLogTypeToRemove) {
        Set<String> set = new HashSet<>(LogTypeEnum.values().length + LogTypeBlockStartEnum.values().length);
        set.addAll(Arrays.stream(LogTypeEnum.values()).map(Object::toString).toList());
        set.addAll(Arrays.stream(LogTypeBlockStartEnum.values()).map(Object::toString).toList());
        if (typeToRemove != null && !typeToRemove.isEmpty()) {
            typeToRemove.stream().map(Object::toString).toList().forEach(set::remove);
        }
        if (startLogTypeToRemove != null && !startLogTypeToRemove.isEmpty()) {
            startLogTypeToRemove.stream().map(Object::toString).toList().forEach(set::remove);
        }
        return set;
    }
}