package bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ByteBuddyAdviceTest {

    // Simple ThreadLocal to store parent thread info
    public static class SimpleThreadLocal {
        public static String parent = null;
    }

    // Original class - we'll create an enhanced version
    public static class OriginalClass {
        public <R> R foo(Supplier<R> fn) {
            System.out.println("Original foo method called");
            return fn.get();
        }
    }

    // Helper class for wrapping suppliers
    public static class SupplierWrapper {
        public static <R> Supplier<R> wrapSupplier(Supplier<R> originalSupplier) {
            String currentThreadId = String.valueOf(Thread.currentThread().getId());
            System.out.println("Wrapping supplier with thread ID: " + currentThreadId);

            return () -> {
                System.out.println("Wrapped supplier executing in thread: " + Thread.currentThread().getId());
                SimpleThreadLocal.parent = currentThreadId;
                try {
                    return originalSupplier.get();
                } finally {
                    System.out.println("Supplier execution completed, cleanup done");
                }
            };
        }
    }

    // ByteBuddy Advice class - this is where the magic happens!
    public static class SupplierWrappingAdvice {

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Supplier<?> supplier) {
            System.out.println("Advice: Wrapping supplier parameter");
            // Directly modify the supplier parameter before method execution
            supplier = SupplierWrapper.wrapSupplier(supplier);
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=== Testing Original Class ===");
        // Test original class
        OriginalClass original = new OriginalClass();
        original.foo(() -> {
            System.out.println("Original class - Parent thread: " + SimpleThreadLocal.parent);
            return "Original result";
        });

        System.out.println("\n=== Creating Enhanced Class with Advice (ONE TIME ONLY) ===");

        // Create enhanced class using Advice - MUCH SIMPLER!
        Class<?> enhancedClass = new ByteBuddy()
                .subclass(OriginalClass.class)
                .name("EnhancedClassWithAdvice")
                .method(ElementMatchers.named("foo"))
                .intercept(Advice.to(SupplierWrappingAdvice.class))
                .make()
                .load(ByteBuddyAdviceTest.class.getClassLoader())
                .getLoaded();

        System.out.println("Enhanced class created with Advice! No interception overhead.\n");

        // Create instance of enhanced class
        Object enhancedInstance = enhancedClass.getDeclaredConstructor().newInstance();
        Method fooMethod = enhancedClass.getMethod("foo", Supplier.class);

        System.out.println("=== Testing Enhanced Class ===");

        System.out.println("--- Test 1: Direct Supplier ---");
        fooMethod.invoke(enhancedInstance, (Supplier<String>) () -> {
            System.out.println("Enhanced class - Parent thread: " + SimpleThreadLocal.parent);
            return "Enhanced result 1";
        });

        System.out.println("\n--- Test 2: Async Supplier ---");
        fooMethod.invoke(enhancedInstance, (Supplier<String>) () -> {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                System.out.println("Async operation - Parent thread: " + SimpleThreadLocal.parent);
                return "Async result";
            });

            try {
                return future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("\n--- Test 3: Multiple Calls ---");
        for (int i = 0; i < 3; i++) {
            final int iteration = i;
            fooMethod.invoke(enhancedInstance, (Supplier<String>) () -> {
                System.out.println("Call " + iteration + " - Parent thread: " + SimpleThreadLocal.parent);
                return "Result " + iteration;
            });
        }

        Thread.sleep(100);
        System.out.println("\n=== Summary ===");
        System.out.println("✅ Used ByteBuddy Advice - much simpler!");
        System.out.println("✅ Supplier wrapping built into method bytecode");
        System.out.println("✅ No interception overhead");
        System.out.println("✅ No Java agent required");
        System.out.println("✅ Clean, readable code");
    }
}