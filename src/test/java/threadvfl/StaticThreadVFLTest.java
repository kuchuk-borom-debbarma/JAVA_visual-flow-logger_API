package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.StaticThreadVFL;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class StaticThreadVFLTest {
    int square(int a) {
        StaticThreadVFL.Log("Squaring " + a);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return StaticThreadVFL.LogFn(() -> a * a, integer -> "Square of " + a + " = " + integer);
    }

    void transaction(String item) {
        StaticThreadVFL.Log("Transaction " + item);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        StaticThreadVFL.Log("Finished transaction " + item);
    }

    int moveToTarget(int num, int target) {
        StaticThreadVFL.Log("Move to target " + num + " from " + target);
        if (target == num) {
            StaticThreadVFL.Log("REached target returning it (" + num + ")");
            return num;
        }
        int returned;
        if (num < target) {
            returned = StaticThreadVFL.Supply(
                    "Move to target block " + num + 1,
                    "Incrementing num to " + num + 1,
                    () -> moveToTarget(num + 1, target),
                    integer -> "Increased num to " + integer
            );
        } else {
            returned = StaticThreadVFL.Supply(
                    "Move to target block " + (num - 1),
                    "Decrement num to " + (num - 1),
                    () -> moveToTarget(num - 1, target),
                    integer -> "Decreased num to " + integer
            );
        }
        return StaticThreadVFL.LogFn(() -> returned, integer -> "Returned " + returned);
    }

    VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + StaticThreadVFLTest.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncVFLBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void flatFlow() {
        ThreadVFLRunner.StartVFL("Flat FLow", createBuffer("flatFlow.json"), () -> {
            StaticThreadVFL.Log("Starting flat flow right now");
            int a = StaticThreadVFL.LogFn(() -> 1 * 2, integer -> "Output is " + integer);
            StaticThreadVFL.Log("Int a = " + a);
        });
    }

    @Test
    void flatNestedFlow() {
        ThreadVFLRunner.StartVFL("Nested FLow", createBuffer("flatNestedFlow.json"), () -> {
            StaticThreadVFL.Log("Starting nested FLow right now");
            int a = 2;
            int finalA = a;
            a = StaticThreadVFL.Supply("Square block", "Starting to sqaure", () -> square(finalA), integer -> "Result is " + integer);
            StaticThreadVFL.Log("Updated a = " + a);
            int finalA1 = a;
            StaticThreadVFL.Run("Transaction Block", "Transactioning " + a, () -> transaction(String.valueOf(finalA1)));
            StaticThreadVFL.Log("Finished nested FLow right now");
        });
    }

    @Test
    void deepNestedFlow() {
        ThreadVFLRunner.StartVFL("Nested FLow", createBuffer("deepNestedFlow.json"), () -> {
            StaticThreadVFL.Log("Starting nested FLow right now");
            int a = 2;
            int target = 5;
            int last = StaticThreadVFL.Supply("Move to target block root ", () -> moveToTarget(a, target));
            StaticThreadVFL.Log("Final is " + last);
        });
    }

    @Test
    void asyncFlow() {
        ThreadVFLRunner.StartVFL("Async FLow", createBuffer("asyncFlow.json"), () -> {
            StaticThreadVFL.Log("Starting async flow right now");
            CompletableFuture<Integer> a = StaticThreadVFL.SupplyAsync("Async Sum", () -> square(2), integer -> "Result is " + integer);
            CompletableFuture<Integer> b = StaticThreadVFL.SupplyAsync("Async Sum2", () -> square(3), integer -> "Result is " + integer);
            int finalA = a.join();
            int finalB = b.join();
            StaticThreadVFL.Log("Final is " + finalA);
            StaticThreadVFL.Log("Final is " + finalB);
            var t1 = StaticThreadVFL.RunAsync("Async rin block", () -> {
                StaticThreadVFL.Log("Starting a rin block right now");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                StaticThreadVFL.Log("Finished a rin block right now");
            });
            var t2 = StaticThreadVFL.RunAsync("Async rin block2 ", () -> {
                StaticThreadVFL.Log("Starting a rin block right now");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                StaticThreadVFL.Log("Finished a rin block right now");
            });
            CompletableFuture.allOf(t2, t2).join();
        });
    }

}
