package dev.kuku.vfl.core.wrappers;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VFLCompletableFuture {
    public static <R> CompletableFuture<R> completedFuture(R value, LogTypeBlockStartEnum logTypeBlockStartEnum) {
        return CompletableFuture.completedFuture(value);
    }

    public static <R> CompletableFuture<R> completedFuture(R value) {
        return CompletableFuture.completedFuture(value);
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, LogTypeBlockStartEnum logTypeBlockStartEnum) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } finally {
                System.out.println("TODO");
            }
        });
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } finally {
                System.out.println("TODO");
            }
        });
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, LogTypeBlockStartEnum logTypeBlockStartEnum) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } finally {
                System.out.println("TODO");
            }
        });
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } finally {
                System.out.println("TODO");
            }
        });
    }
}
