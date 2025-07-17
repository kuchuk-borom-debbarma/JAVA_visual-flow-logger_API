package dev.kuku;


import dev.kuku.vfl.core.BaseLogger;
import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.executionLogger.ExecutionLogger;
import dev.kuku.vfl.executionLogger.ExecutionLoggerRunner;
import dev.kuku.vfl.scopedLogger.ScopedLogger;
import dev.kuku.vfl.scopedLogger.ScopedLoggerImpl;
import dev.kuku.vfl.scopedLogger.ScopedLoggerRunner;

import java.util.Scanner;

public class Main {
    static final InMemoryFlushHandlerImpl inMemory = new InMemoryFlushHandlerImpl();
    static final VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(10, 10, inMemory);

    public static void main(String... args) {
        new ScopedLoggerExample().run();
        System.out.println(inMemory.toJsonNested());
    }

    static class ExecutionLoggerExample {
        void run() {
            ExecutionLoggerRunner.run("ExecutionLoggerExample", buffer, logger -> {
                logger.text("Started ExecutionLoggerExample");
                int input = new Scanner(System.in).nextInt();
                logger.text("Input: " + input);
                logger.text("Calculating square");
                int squared = logger.call("Square from root", "Squaring", logger1 -> square(input, logger1));
                logger.text("Squared: " + squared);
                logger.text("Calculating square and split of updated data");
                int finalVal = logger.call("Splitting From root block", null, integer -> String.format("Square and split result = %s", integer), logger1 -> squareAndSplit(squared, logger1));
                logger.text("Final: " + finalVal);
            });
        }

        int split(int num, BaseLogger logger) {
            logger.text("Splitting " + num);
            int result = num / 2;
            logger.text("Result: " + result);
            return result;
        }

        int square(int num, BaseLogger logger) {
            logger.text("Squaring " + num);
            int result = num * num;
            logger.text("Result: " + result);
            return result;
        }

        int squareAndSplit(int num, ExecutionLogger logger) {
            logger.text("Squaring and then multiplying " + num);
            int result = logger.call("Square", "Summing", s -> String.format("calculated square = %s", s), (subLogger) -> square(num, subLogger));

            int finalResult = result;
            result = logger.call("Split", "Splitting " + result, half -> String.format("After splitting : " + half), sl -> split(finalResult, sl));
            logger.text("Result: " + result);
            return result;
        }
    }

    static class ScopedLoggerExample {
        ScopedLogger logger;

        void run() {
            ScopedLoggerRunner.run("ScopedLoggerExample", buffer, () -> {
                logger = ScopedLoggerImpl.get();
                logger.text("Started ExecutionLoggerExample");
                int input = new Scanner(System.in).nextInt();
                logger.text("Input: " + input);
                logger.text("Calculating square");
                int squared = logger.call("Square from root", "Squaring", () -> square(input));
                logger.text("Squared: " + squared);
                logger.text("Calculating square and split of updated data");
                int finalVal = logger.call("Splitting From root block",
                        null,
                        integer -> String.format("Square and split result = %s", integer),
                        () -> squareAndSplit(squared));
                logger.text("Final: " + finalVal);
            });
        }

        int split(int num) {
            logger.text("Splitting " + num);
            int result = num / 2;
            logger.text("Result: " + result);
            return result;
        }

        int square(int num) {
            logger.text("Squaring " + num);
            int result = num * num;
            logger.text("Result: " + result);
            return result;
        }

        int squareAndSplit(int num) {
            logger.text("Squaring and then multiplying " + num);
            int result = logger.call("Square", "Summing", s -> String.format("calculated square = %s", s), () -> square(num));

            int finalResult = result;
            result = logger.call("Split", "Splitting " + result, half -> String.format("After splitting : " + half), () -> split(finalResult));
            logger.text("Result: " + result);
            return result;
        }
    }
}
