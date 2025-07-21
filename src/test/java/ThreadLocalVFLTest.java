import dev.kuku.vfl.core.buffer.ThreadSafeSynchronousVflBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.InMemoryFlushHandlerImpl;
import dev.kuku.vfl.threadLocal.IThreadLocal;
import dev.kuku.vfl.threadLocal.ThreadLocaVFL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ThreadLocalVFLTest {

    private InMemoryFlushHandlerImpl memory;
    private VFLBuffer buffer;
    private ExecutorService executor;
    private final AtomicInteger testCounter = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        memory = new InMemoryFlushHandlerImpl();
        buffer = new ThreadSafeSynchronousVflBuffer(100, 100, memory);
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (FileWriter f = new FileWriter("comprehensive_test_" + testCounter.incrementAndGet() + ".json")) {
            f.write(memory.toJsonNested());
            memory.cleanup();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    @DisplayName("Deep Nested Calls Test")
    void deepNestedCallsTest() {
        IThreadLocal.Runner.call("Deep Nested Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Starting deep nested test");

            int result = l.call("Level 1", "First level",
                () -> deepNestedOperation(1, 5),
                r -> "Level 1 result: " + r);

            l.msg("Final nested result: " + result);
            return null;
        });
    }

    private int deepNestedOperation(int currentLevel, int maxLevel) {
        var l = ThreadLocaVFL.Get();
        l.msg("Entering level " + currentLevel);

        if (currentLevel >= maxLevel) {
            l.msg("Reached max level, returning base value");
            return currentLevel * 10;
        }

        return l.call("Level " + (currentLevel + 1),
            "Processing level " + (currentLevel + 1),
            () -> {
                // Add some computation
                int intermediate = currentLevel * 2;
                l.msg("Intermediate calculation: " + intermediate);
                return deepNestedOperation(currentLevel + 1, maxLevel) + intermediate;
            },
            result -> "Level " + currentLevel + " computed: " + result);
    }

    @Test
    @DisplayName("Complex Async Operations Test")
    void complexAsyncOperationsTest() {
        IThreadLocal.Runner.call("Complex Async Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Starting complex async operations");

            // Create multiple async tasks with different durations
            var fastTask = l.callAsync("Fast Task", "Quick operation",
                () -> simulateWork(100, "Fast"),
                r -> "Fast result: " + r,
                executor);

            var mediumTask = l.callAsync("Medium Task", "Medium operation",
                () -> simulateWork(500, "Medium"),
                r -> "Medium result: " + r,
                executor);

            var slowTask = l.callAsync("Slow Task", "Slow operation",
                () -> simulateWork(1000, "Slow"),
                r -> "Slow result: " + r,
                executor);

            // Process results in different orders
            l.msg("Waiting for medium task first");
            var mediumResult = l.msgFn(mediumTask::get, r -> "Got medium: " + r);

            l.msg("Now waiting for fast task");
            var fastResult = l.msgFn(fastTask::get, r -> "Got fast: " + r);

            l.msg("Finally waiting for slow task");
            var slowResult = l.msgFn(slowTask::get, r -> "Got slow: " + r);

            l.msg("All tasks completed: " + fastResult + ", " + mediumResult + ", " + slowResult);
            return null;
        });
    }

    @Test
    @DisplayName("Nested Async Within Sync Test")
    void nestedAsyncWithinSyncTest() {
        IThreadLocal.Runner.call("Nested Async in Sync", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Starting nested async within sync test");

            return l.call("Outer Sync Block", "Processing outer sync", () -> {
                l.msg("Inside outer sync block");

                var asyncTask1 = l.callAsync("Inner Async 1", "First inner async",
                    () -> {
                        // Nested sync call within async
                        return l.call("Sync in Async 1", "Sync operation in async",
                            () -> simulateWork(300, "SyncInAsync1"),
                            r -> "Nested sync result: " + r);
                    },
                    r -> "Async 1 final: " + r,
                    executor);

                var asyncTask2 = l.callAsync("Inner Async 2", "Second inner async",
                    () -> simulateWork(200, "Async2"),
                    r -> "Async 2 result: " + r,
                    executor);

                // Another sync operation while async tasks are running
                int syncResult = l.call("Parallel Sync", "Sync while async runs",
                    () -> simulateWork(150, "ParallelSync"),
                    r -> "Parallel sync: " + r);

                // Collect async results
                var result1 = l.msgFn(asyncTask1::get, r -> "Collected async 1: " + r);
                var result2 = l.msgFn(asyncTask2::get, r -> "Collected async 2: " + r);

                return syncResult + result1 + result2;
            }, finalResult -> "Outer block final: " + finalResult);
        });
    }

    @Test
    @DisplayName("Exception Handling in Nested Calls")
    void exceptionHandlingTest() {
        IThreadLocal.Runner.call("Exception Handling Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Testing exception handling in nested calls");

            try {
                l.call("Exception Test Block", "This will throw an exception", () -> {
                    l.msg("About to call method that throws exception");
                    return methodThatThrows();
                }, r -> "Should not reach this: " + r);
            } catch (RuntimeException e) {
                l.msg("Caught expected exception: " + e.getMessage());
            }

            // Test recovery after exception
            int recoveryResult = l.call("Recovery Block", "Recovering from exception",
                () -> simulateWork(100, "Recovery"),
                r -> "Recovery successful: " + r);

            l.msg("Recovery result: " + recoveryResult);
            return null;
        });
    }

    @Test
    @DisplayName("Async Exception Handling")
    void asyncExceptionHandlingTest() {
        IThreadLocal.Runner.call("Async Exception Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Testing async exception handling");

            var successTask = l.callAsync("Success Task", "This should succeed",
                () -> simulateWork(200, "Success"),
                r -> "Success: " + r,
                executor);

            var failureTask = l.callAsync("Failure Task", "This will fail",
                this::methodThatThrows,
                r -> "Should not see this: " + r,
                executor);

            // Handle successful task
            var successResult = l.msgFn(successTask::get, r -> "Got success: " + r);
            l.msg("Success result: " + successResult);

            // Handle failing task
            try {
                var failureResult = l.msgFn(failureTask::get, r -> "Should not reach: " + r);
            } catch (Exception e) {
                l.msg("Caught async exception: " + e.getCause().getMessage());
            }

            return null;
        });
    }

    @Test
    @DisplayName("Concurrent Multiple Contexts")
    void concurrentMultipleContextsTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Start multiple concurrent contexts
        for (int i = 0; i < 3; i++) {
            final int contextId = i;
            new Thread(() -> {
                try {
                    IThreadLocal.Runner.call("Context " + contextId, buffer, () -> {
                        var l = ThreadLocaVFL.Get();
                        l.msg("Starting context " + contextId);

                        // Each context does nested and async work
                        var asyncTask = l.callAsync("Context " + contextId + " Async",
                            "Async work in context " + contextId,
                            () -> l.call("Nested in " + contextId,
                                "Nested sync in async context " + contextId,
                                () -> simulateWork(200 + contextId * 100, "Context" + contextId),
                                r -> "Context " + contextId + " nested: " + r),
                            r -> "Context " + contextId + " async final: " + r,
                            executor);

                        var result = l.msgFn(asyncTask::get, r -> "Context " + contextId + " result: " + r);
                        l.msg("Context " + contextId + " completed with: " + result);
                        return null;
                    });
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            throw new RuntimeException("Exceptions occurred in concurrent test", exceptions.getFirst());
        }
    }

    @Test
    @DisplayName("Resource Cleanup Test")
    void resourceCleanupTest() {
        IThreadLocal.Runner.call("Resource Cleanup Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Testing resource cleanup scenarios");

            // Test with try-with-resources pattern simulation
            return l.call("Resource Block", "Using resources", () -> {
                l.msg("Acquiring resources");

                var resource1 = l.callAsync("Resource 1", "Acquiring resource 1",
                    () -> {
                        l.msg("Resource 1 acquired, doing work");
                        return simulateWork(300, "Resource1");
                    },
                    r -> "Resource 1 ready: " + r,
                    executor);

                var resource2 = l.callAsync("Resource 2", "Acquiring resource 2",
                    () -> {
                        l.msg("Resource 2 acquired, doing work");
                        return simulateWork(250, "Resource2");
                    },
                    r -> "Resource 2 ready: " + r,
                    executor);

                try {
                    var result1 = l.msgFn(resource1::get, r -> "Using resource 1: " + r);
                    var result2 = l.msgFn(resource2::get, r -> "Using resource 2: " + r);

                    l.msg("Both resources used successfully");
                    return result1 + result2;
                } finally {
                    l.msg("Cleaning up resources");
                }
            }, result -> "Resource cleanup completed: " + result);
        });
    }

    @Test
    @DisplayName("Stress Test with Random Operations")
    void stressTestRandomOperations() {
        IThreadLocal.Runner.call("Stress Test", buffer, () -> {
            var l = ThreadLocaVFL.Get();
            l.msg("Starting stress test with random operations");

            Random random = new Random(12345); // Fixed seed for reproducibility
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                final int operationId = i;
                var future = l.callAsync("Random Op " + operationId,
                    "Random operation " + operationId,
                    () -> randomOperation(operationId, random.nextInt(3) + 1, random),
                    r -> "Random op " + operationId + " result: " + r,
                    executor);
                futures.add(future);
            }

            // Collect results in random order
            Collections.shuffle(futures, random);
            int totalSum = 0;
            for (var future : futures) {
                int result = l.msgFn(future::get, r -> "Collected random result: " + r);
                totalSum += result;
            }

            l.msg("Stress test completed, total sum: " + totalSum);
            return null;
        });
    }

    // Helper methods

    private int simulateWork(int durationMs, String workId) {
        var l = ThreadLocaVFL.Get();
        l.msg("Starting work: " + workId + " (duration: " + durationMs + "ms)");

        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Work interrupted: " + workId, e);
        }

        int result = workId.hashCode() % 1000;
        l.msg("Completed work: " + workId + " with result: " + result);
        return result;
    }

    private int methodThatThrows() {
        var l = ThreadLocaVFL.Get();
        l.msg("This method is about to throw an exception");
        throw new RuntimeException("Intentional exception for testing");
    }

    private int randomOperation(int operationId, int depth, Random random) {
        var l = ThreadLocaVFL.Get();
        l.msg("Random operation " + operationId + " at depth " + depth);

        if (depth <= 0) {
            return simulateWork(random.nextInt(200) + 50, "RandomLeaf" + operationId);
        }

        return l.call("Random Nested " + operationId + "-" + depth,
            "Random nested operation " + operationId + " depth " + depth,
            () -> {
                int result = simulateWork(random.nextInt(100) + 25, "RandomNested" + operationId + "-" + depth);
                if (random.nextBoolean()) {
                    // Sometimes add another level of nesting
                    result += randomOperation(operationId * 10 + depth, depth - 1, random);
                }
                return result;
            },
            r -> "Random nested " + operationId + "-" + depth + " result: " + r);
    }
}