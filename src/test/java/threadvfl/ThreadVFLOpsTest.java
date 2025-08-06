package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLOps;
import dev.kuku.vfl.impl.threadlocal.logger.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class ThreadVFLOpsTest {
    int square(int a) {
        ThreadVFLOps.Log("Squaring " + a);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ThreadVFLOps.LogFn(() -> a * a, integer -> "Square of " + a + " = " + integer);
    }

    void transaction(String item) {
        ThreadVFLOps.Log("Transaction " + item);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ThreadVFLOps.Log("Finished transaction " + item);
    }

    int moveToTarget(int num, int target) {
        ThreadVFLOps.Log("Move to target " + num + " from " + target);
        if (target == num) {
            ThreadVFLOps.Log("REached target returning it (" + num + ")");
            return num;
        }
        int returned;
        if (num < target) {
            returned = ThreadVFLOps.Supply(
                    "Move to target block " + num + 1,
                    "Incrementing num to " + num + 1,
                    () -> moveToTarget(num + 1, target),
                    integer -> "Increased num to " + integer
            );
        } else {
            returned = ThreadVFLOps.Supply(
                    "Move to target block " + (num - 1),
                    "Decrement num to " + (num - 1),
                    () -> moveToTarget(num - 1, target),
                    integer -> "Decreased num to " + integer
            );
        }
        return ThreadVFLOps.LogFn(() -> returned, integer -> "Returned " + returned);
    }

    VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + ThreadVFLOpsTest.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void flatFlow() {
        ThreadVFLRunner.StartVFL("Flat FLow", createBuffer("flatFlow.json"), () -> {
            ThreadVFLOps.Log("Starting flat flow right now");
            int a = ThreadVFLOps.LogFn(() -> 1 * 2, integer -> "Output is " + integer);
            ThreadVFLOps.Log("Int a = " + a);
        });
    }

    @Test
    void flatNestedFlow() {
        ThreadVFLRunner.StartVFL("Nested FLow", createBuffer("flatNestedFlow.json"), () -> {
            ThreadVFLOps.Log("Starting nested FLow right now");
            int a = 2;
            int finalA = a;
            a = ThreadVFLOps.Supply("Square block", "Starting to sqaure", () -> square(finalA), integer -> "Result is " + integer);
            ThreadVFLOps.Log("Updated a = " + a);
            int finalA1 = a;
            ThreadVFLOps.Run("Transaction Block", "Transactioning " + a, () -> transaction(String.valueOf(finalA1)));
            ThreadVFLOps.Log("Finished nested FLow right now");
        });
    }

    @Test
    void deepNestedFlow() {
        ThreadVFLRunner.StartVFL("Nested FLow", createBuffer("deepNestedFlow.json"), () -> {
            ThreadVFLOps.Log("Starting nested FLow right now");
            int a = 2;
            int target = 5;
            int last = ThreadVFLOps.Supply("Move to target block root ", () -> moveToTarget(a, target));
            ThreadVFLOps.Log("Final is " + last);
        });
    }

    @Test
    void asyncFlow() {
        ThreadVFLRunner.StartVFL("Async FLow", createBuffer("asyncFlow.json"), () -> {
            ThreadVFLOps.Log("Starting async flow right now");
            CompletableFuture<Integer> a = ThreadVFLOps.SupplyAsync("Async Sum", () -> square(2), integer -> "Result is " + integer);
            CompletableFuture<Integer> b = ThreadVFLOps.SupplyAsync("Async Sum2", () -> square(3), integer -> "Result is " + integer);
            int finalA = a.join();
            int finalB = b.join();
            ThreadVFLOps.Log("Final is " + finalA);
            ThreadVFLOps.Log("Final is " + finalB);
            var t1 = ThreadVFLOps.RunAsync("Async rin block", () -> {
                ThreadVFLOps.Log("Starting a rin block right now");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ThreadVFLOps.Log("Finished a rin block right now");
            });
            var t2 = ThreadVFLOps.RunAsync("Async rin block2 ", () -> {
                ThreadVFLOps.Log("Starting a rin block right now");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ThreadVFLOps.Log("Finished a rin block right now");
            });
            CompletableFuture.allOf(t2, t2).join();
        });
    }

}
