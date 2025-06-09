package dev.kuku;

import dev.kuku.vfl.VflBlockOperator;
import dev.kuku.vfl.VflClientBuilder;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        var client = VflClientBuilder.start().build();
        client.startRootBlock("Root Block", Main::rootOperation);
        var json = client.buffer.flushToJSON();
        System.out.println(json);
    }

    public static void rootOperation(VflBlockOperator vfl) {
        vfl = vfl.log("Root operation started");

        // Fire-and-forget async operation
        vfl.log(
                Main::asyncOperation,
                "Running Async operation",
                () -> "Fire and forget operation complete",
                "Fire and Forget Operation"
        );

        // Continue with root operation (prints before async completes)
        vfl = vfl.log("Root operation continuing while async runs...");
        vfl.log("Root operation finished");
    }

    public static void asyncOperation(VflBlockOperator a) {
        CompletableFuture.runAsync(() -> {
            VflBlockOperator current = a.log("Async operation started");
            try {
                Thread.sleep(2000); // Simulate async work
                current.log("Async operation completed after 2 seconds");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}