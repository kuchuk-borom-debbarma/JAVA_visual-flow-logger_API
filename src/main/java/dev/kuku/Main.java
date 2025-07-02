package dev.kuku;

import dev.kuku.vfl.BlockLogger;
import dev.kuku.vfl.VFL;
import dev.kuku.vfl.buffer.SynchronousBuffer;
import dev.kuku.vfl.models.VflLogType;

public class Main {
    static VFL vfl = new VFL(new SynchronousBuffer(10000));

    public static void main(String... args) {
        new SimpleFlow().start();
    }

    static class SimpleFlow {
        void start() {
            vfl.start("Order nike sync", logger -> {
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

            logger.logSubProcess("Write Order history", "Writing order history", this::writeTransaction, false);
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