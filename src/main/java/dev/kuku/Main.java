package dev.kuku;

import dev.kuku.vfl.buffer.DefaultBufferImpl;
import dev.kuku.vfl.models.VflBlockDataType;
import dev.kuku.vfl.StartedVFL;

import java.util.UUID;

public class Main {
    //TODO add a common output so that the results of dao can be used to give us the same output. This will help the diagram loader to work flawlessly. Need to figure out how to extract them and when. Or In client have different types of loader that can load from different source. This one is easier but more work.
    public static void main(String[] args) {
        var mainLogger = new StartedVFL(new VflBlockDataType(null, UUID.randomUUID().toString(), "MAIN"), new DefaultBufferImpl(5, 5));
        testSimple(mainLogger);
        mainLogger.shutdown();

    }

    //We need to update VFLBlockOperator per log
    public static void testSimple(StartedVFL logger) {
        logger.log("Test Simple stated");
        int sum = logger.logWithResult("SUM", l -> sum(1, 2, l), integer -> "Result is " + integer);
        logger.log("so after sum we have " + sum);
        logger.log("Test Simple completed");
    }

    public static int sum(int a, int b, StartedVFL logger) {
        logger.log("Adding " + a + " and " + b);
        int result = a + b;
        logger.log("Result is " + result);
        return result;
    }

}