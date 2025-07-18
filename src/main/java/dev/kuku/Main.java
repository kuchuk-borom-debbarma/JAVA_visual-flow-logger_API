package dev.kuku;


import dev.kuku.vfl.contextualVFLogger.ContextualVFL;
import dev.kuku.vfl.contextualVFLogger.ContextualVFLRunner;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVirtualThreadVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLRunner;
import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;

import java.util.Scanner;
import java.util.concurrent.Future;

public class Main {
    static final InMemoryFlushHandlerImpl inMemory = new InMemoryFlushHandlerImpl();
    static final VFLBuffer buffer = new ThreadSafeAsyncVirtualThreadVFLBuffer(1, 1, inMemory);
    static final ScopedValue<String> test = ScopedValue.newInstance();

    public static void main(String... args) {

    }

    static class ContextualLoggerAsyncExample {
        void run() {
            ContextualVFLRunner.run("Async contextual logger example", buffer, logger -> {
            });
        }

        Future<Void> task(VFL logger) {
            logger.text("Starting task1 in thread " + Thread.currentThread().getName());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.error("Interrupted thread " + Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
            logger.text("Finished task1 in thread " + Thread.currentThread().getName());
            return null;
        }
    }

    static class ExecutionLoggerExample {
        void run() {
            ContextualVFLRunner.run("ExecutionLoggerExample", buffer, logger -> {
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

        int split(int num, VFL logger) {
            logger.text("Splitting " + num);
            int result = num / 2;
            logger.text("Result: " + result);
            return result;
        }

        int square(int num, VFL logger) {
            logger.text("Squaring " + num);
            int result = num * num;
            logger.text("Result: " + result);
            return result;
        }

        int squareAndSplit(int num, ContextualVFL logger) {
            logger.text("Squaring and then multiplying " + num);
            int result = logger.call("Square", "Summing", s -> String.format("calculated square = %s", s), (subLogger) -> square(num, subLogger));

            int finalResult = result;
            result = logger.call("Split", "Splitting " + result, half -> String.format("After splitting : " + half), sl -> split(finalResult, sl));
            logger.text("Result: " + result);
            return result;
        }
    }

    static class ScopedLoggerExample {
        ScopedVFL logger;

        void run() {
            ScopedVFLRunner.run("ScopedLoggerExample", buffer, () -> {
                logger = ScopedVFLImpl.get();
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
