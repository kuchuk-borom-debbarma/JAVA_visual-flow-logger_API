package dev.kuku;

import dev.kuku.vfl.VflBlockOperator;
import dev.kuku.vfl.VflClientBuilder;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        var client = VflClientBuilder.start().build();
        client.startRootBlock("Root Block", Main::rootOperation);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var json = client.buffer.flushToJSON();
        System.out.println(json);
    }

    public static void rootOperation(VflBlockOperator vfl) {
        vfl.log("Starting operations");

        // 1. Fire-and-forget with return value in postBlockMessage
        vfl.logAsync(
                Main::fetchData,
                "Fetching data",
                result -> "Got: " + result,
                "Fetch Data"
        );

        // 2. Parallel operations (like Promise.all)
        CompletableFuture<String> task1 = vfl.logAsync(Main::taskA, "Task A", result -> "A: " + result, "Task A");
        CompletableFuture<Integer> task2 = vfl.logAsync(Main::taskB, "Task B", result -> "B: " + result, "Task B");

        // Wait for both and use results
        vfl.log(blockOp -> {
            String a = task1.join();
            Integer b = task2.join();
            blockOp.log("Both completed: " + a + ", " + b);
            return "Combined: " + a + "-" + b;
        }, "Waiting for parallel tasks", result -> result, "Combine Results");

        // 3. Chain async calls using return value
        CompletableFuture<Double> price = vfl.logAsync(Main::getPrice, "Getting price", p -> "Price: $" + p, "Get Price");
        vfl.logAsync(
                blockOp -> price.thenCompose(p -> processPayment(blockOp, p)),
                "Processing payment",
                success -> "Payment: " + (success ? "OK" : "Failed"),
                "Process Payment"
        );

        vfl.log("Operations started");
    }

    // Simple async operations
    public static CompletableFuture<String> fetchData(VflBlockOperator vfl) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "UserData123";
        });
    }

    public static CompletableFuture<String> taskA(VflBlockOperator vfl) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "ResultA";
        });
    }

    public static CompletableFuture<Integer> taskB(VflBlockOperator vfl) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1200);
            return 42;
        });
    }

    public static CompletableFuture<Double> getPrice(VflBlockOperator vfl) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return 99.99;
        });
    }

    public static CompletableFuture<Boolean> processPayment(VflBlockOperator vfl, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return amount > 0;
        });
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}