package dev.kuku;

import dev.kuku.vfl.core.logger.BlockLogger;
import dev.kuku.vfl.VFL;
import dev.kuku.vfl.core.buffer.AsynchronousBuffer;
import dev.kuku.vfl.core.serviceCall.NaiveVFLServerAPI;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class Main {
    static VFL vfl = new VFL(new AsynchronousBuffer(1, 1, 2, new NaiveVFLServerAPI("http://localhost:8080")));

    public static void main(String... args) {
        new AsyncFlowTest(vfl).run();
    }

    record SimpleLinearFlow(VFL vfl) {
        public void run() {
            vfl.start("Simple linear flow ", this::startingPoint);
        }

        private void startingPoint(BlockLogger logger) {
            logger.message("Starting simple linear flow");
            logger.message("Taking user input");
            int num1 = new Scanner(System.in).nextInt();
            logger.message("num1 = " + num1);
            logger.runBlockResult("Square", "Squaring num1 now",
                    o -> String.format("Square of %d us %d", num1, o),
                    logger1 -> square(num1, logger1));
            logger.runBlock("Store Calculating History", l -> storeCalculatingHistory("Square of " + num1, l));
            logger.message("Operation completed");
        }

        private int square(int num, BlockLogger logger) {
            logger.message("Square of " + num);
            return num * num;
        }

        private void storeCalculatingHistory(String history, BlockLogger logger) {
            logger.message("Storing calculating history = " + history);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.message("Stored history = " + history);
        }
    }

    record AsyncFlowTest(VFL vfl) {
        public void run() {
            vfl.start("Async flow test", this::startingPoint);
        }

        public void startingPoint(BlockLogger logger) {
            logger.message("Starting async flow test");
            var sumTask = CompletableFuture.supplyAsync(() -> logger.runBlockResultAndStay("Sum",
                    "Sum of 1, 2",
                    o -> "Sum = " + o,
                    l -> sum(1, 2, l)), Executors.newVirtualThreadPerTaskExecutor());
            var multiplyTask = CompletableFuture.supplyAsync(() -> logger.runBlockResultAndStay("Multiply",
                    "1*2",
                    o -> "1*2 = " + o,
                    l -> multiply(1, 2, l)), Executors.newVirtualThreadPerTaskExecutor());

            var r1 = sumTask.join();    // Wait for Task 1 to finish
            var r2 = multiplyTask.join(); // Wait for Task 2 to finish

            logger.message("r1 = " + r1 + " r2 = " + r2);

        }

        // In your sum method, add some logging
        int sum(int a, int b, BlockLogger logger) {
            logger.message("Sum of " + a + " and " + b);
            logger.message("calculating...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("InterruptedException in sum: " + e.getMessage());
                throw new RuntimeException(e);
            }
            return a + b;
        }

        int multiply(int a, int b, BlockLogger logger) {
            logger.message("Multiply of " + a + " and " + b);
            logger.message("calculating...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return a * b;
        }
    }
}




