package dev.kuku;

import dev.kuku.vfl.api.buffer.DefaultBufferImpl;
import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.VisFlowLogger;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        var mainLogger = new VisFlowLogger(new VflBlockDataType(null, UUID.randomUUID().toString(), "MAIN"), new DefaultBufferImpl(5, 5));
        testSimple(mainLogger);
        mainLogger.shutdown();

    }

    //We need to update VFLBlockOperator per log
    public static void testSimple(VisFlowLogger logger) {
        logger.log("Test Simple stated");
        int sum = logger.logWithResult("SUM", l -> sum(1, 2, l), integer -> "Result is " + integer);
        logger.log("so after sum we have " + sum);
        logger.log("Test Simple completed");
    }

    public static int sum(int a, int b, VisFlowLogger logger) {
        logger.log("Adding " + a + " and " + b);
        int result = a + b;
        logger.log("Result is " + result);
        return result;
    }

}