package dev.kuku;

import dev.kuku.vfl.BlockLogger;
import dev.kuku.vfl.VFL;
import dev.kuku.vfl.buffer.SynchronousBuffer;
import dev.kuku.vfl.serviceCall.NaiveVFLServerAPI;

import java.util.Scanner;

public class Main {
    static VFL vfl = new VFL(new SynchronousBuffer(10000, new NaiveVFLServerAPI("http://localhost:8080")));

    public static void main(String... args) {
        new SimpleLinearFlow(vfl).run();
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
}




