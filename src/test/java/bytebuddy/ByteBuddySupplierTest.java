package bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ByteBuddySupplierTest {

    public static void main(String[] args) throws Exception {

        // Simple approach - create a new class with the same method signature
        Class<?> enhancedClass = new ByteBuddy()
                .subclass(Object.class)
                .name("EnhancedClass")
                .defineMethod("foo", Object.class, net.bytebuddy.description.modifier.Visibility.PUBLIC)
                .withParameter(Supplier.class, "fn")
                .intercept(MethodDelegation.to(SupplierInterceptor.class))
                .make()
                .load(ByteBuddySupplierTest.class.getClassLoader())
                .getLoaded();

        // Create instance
        Object enhancedInstance = enhancedClass.getDeclaredConstructor().newInstance();
        Method fooMethod = enhancedClass.getMethod("foo", Supplier.class);

        System.out.println("=== Test 1: Direct Supplier ===");
        // Test 1: Direct supplier call
        fooMethod.invoke(enhancedInstance, (Supplier<String>) () -> {
            System.out.println("Direct supplier - Parent thread: " + SimpleThreadLocal.parent);
            return "Direct result";
        });

        System.out.println("\n=== Test 2: Async Supplier ===");
        // Test 2: Supplier that will run in different thread
        fooMethod.invoke(enhancedInstance, (Supplier<String>) () -> {
            // Simulate what happens when this runs in a different thread
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

        Thread.sleep(100);
        System.out.println("Test completed");
    }

    // Simple ThreadLocal to store parent thread info
    public static class SimpleThreadLocal {
        public static String parent = null;
    }

    // Original class - make it public
    public static class OriginalClass {
        public <R> R foo(Supplier<R> fn) {
            System.out.println("Original foo method called");
            return fn.get();
        }
    }

    // Public static interceptor class
    public static class SupplierInterceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args) {
            System.out.println("Intercepted method via static delegation");

            // Find the Supplier parameter and wrap it
            Supplier<?> wrappedSupplier = null;
            for (Object arg : args) {
                if (arg instanceof Supplier) {
                    System.out.println("Found Supplier parameter, wrapping it...");
                    wrappedSupplier = wrapSupplier((Supplier<?>) arg);
                    break;
                }
            }

            // Create original instance and call it
            OriginalClass original = new OriginalClass();
            return original.foo(wrappedSupplier);
        }

        private static <R> Supplier<R> wrapSupplier(Supplier<R> originalSupplier) {
            String currentThreadId = String.valueOf(Thread.currentThread().getId());

            return () -> {
                System.out.println("Wrapped supplier executing in thread: " + Thread.currentThread().getId());
                SimpleThreadLocal.parent = currentThreadId;
                return originalSupplier.get();
            };
        }
    }
}