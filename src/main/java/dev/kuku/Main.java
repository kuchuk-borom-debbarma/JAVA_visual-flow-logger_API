package dev.kuku;

import dev.kuku.vfl.BlockLogger;
import dev.kuku.vfl.VFL;
import dev.kuku.vfl.buffer.SynchronousBuffer;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class Main {
    static VFL vfl = new VFL(new SynchronousBuffer(10));

    public static void main(String... args) {
        vfl.start("Time test", logger -> {
            logger.log("Starting test at time " + Instant.now().toEpochMilli(), VflLogType.MESSAGE, true);
            logger.log("Before sleep", VflLogType.MESSAGE, true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.log("After sleep", VflLogType.MESSAGE, true);
            logger.log("Ending test at time " + Instant.now().toEpochMilli(), VflLogType.MESSAGE, true);
        });
    }

    static class SimpleFlow {
        void orderProgram() {
            vfl.start("Order nike 2", logger -> {
                logger.log("Attempt to order Nike started", VflLogType.MESSAGE, true);
                var canOrder = logger.logSubProcess("Inventory Check", "Checking if shoe is in inventory", this::checkInventory, true);
                if (!canOrder) {
                    logger.log("Can't order as it is not in inventory", VflLogType.MESSAGE, true);
                    return;
                }
                logger.log("Can order, stock in inventory", VflLogType.MESSAGE, true);
                logger.log("Ordering....", VflLogType.MESSAGE, true);
                logger.logSubProcess("Order operation", "Starting order process", this::order, true);
            });
        }

        boolean checkInventory(BlockLogger logger) {
            logger.log("Checking if nike is in inventory", VflLogType.MESSAGE, true);
            var exists = logger.logSubProcess("Database check", "Checking if shoe is in inventory", this::dbCheck, true);
            if (exists) {
                logger.log("Inventory check completed, Exists in database", VflLogType.MESSAGE, true);
                return true;
            } else {
                logger.log("Doesn't exist in Db", VflLogType.MESSAGE, true);
                return false;
            }
        }

        boolean dbCheck(BlockLogger logger) {
            logger.log("SELECT * FROM inventories where name=nike", VflLogType.MESSAGE, true);
            logger.log("Found 200 nike shoes in stock", VflLogType.MESSAGE, true);
            return true;
        }

        void order(BlockLogger logger) {
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            CountDownLatch latch = new CountDownLatch(1);

            logger.logSubProcess("Write Order history", "Writing order history", l -> {
                executor.execute(() -> {
                    try {
                        writeTransaction(l);
                    } finally {
                        latch.countDown();
                    }
                });
            }, false);

            try {
                latch.await(); // Wait for the write operation to complete
            } catch (InterruptedException e) {
                logger.log("Write transaction interrupted: " + e.getMessage(), VflLogType.EXCEPTION, true);
                Thread.currentThread().interrupt();
            } finally {
                executor.close();
            }

            logger.log("Removing item from inventory", VflLogType.MESSAGE, true);
            logger.logSubProcess("DbRemove", "Removing from db", this::dbRemove, true);
        }

        void writeTransaction(BlockLogger logger) {
            logger.log("Writing in database", VflLogType.MESSAGE, true);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException _) {
            }
            logger.log("Writing transaction complete", VflLogType.MESSAGE, true);
        }

        void dbRemove(BlockLogger logger) {
            logger.log("Removing item from inventory", VflLogType.MESSAGE, true);
            try {
                Thread.sleep(500);
            } catch (InterruptedException _) {
            }
            logger.log("Removed item from inventory", VflLogType.MESSAGE, true);
        }

    }
}