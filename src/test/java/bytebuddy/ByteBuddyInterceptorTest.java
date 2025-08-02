package bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

public class ByteBuddyInterceptorTest {

    // Custom annotation to mark methods for interception
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Monitored {
    }

    // Sample service class for testing - made public and static
    public static class TestService {

        @Monitored
        public String processData(String input, int count) {
            return "Processed: " + input + " x" + count;
        }

        @Monitored
        public void calculateSomething(double value, String type) {
            System.out.println("Calculating " + type + " for value: " + value);
        }

        // This method will NOT be intercepted (no @Monitored annotation)
        public void doSomething(String message) {
            System.out.println("Doing: " + message + " (not monitored)");
        }

        @Monitored
        public int addNumbers(int a, int b) {
            return a + b;
        }

        // Another non-monitored method
        public String regularMethod() {
            return "This is a regular method";
        }

        @Monitored
        public void throwException() {
            throw new RuntimeException("Test exception");
        }
    }

    // Interceptor class that handles the annotated method calls - made public and static
    public static class MethodInterceptor {

        @RuntimeType
        public static Object intercept(@Origin Method method,
                                       @AllArguments Object[] args,
                                       @SuperCall Callable<?> callable) throws Exception {

            // Print method start information
            System.out.println("Method: " + method.getName() +
                    " | Parameters: " + Arrays.toString(args) +
                    " | Start");

            long startTime = System.currentTimeMillis();

            try {
                // Call the original method
                Object result = callable.call();

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                // Print method end information with execution time
                System.out.println("Method: " + method.getName() +
                        " | Parameters: " + Arrays.toString(args) +
                        " | End | Duration: " + duration + "ms");

                return result;
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                System.out.println("Method: " + method.getName() +
                        " | Parameters: " + Arrays.toString(args) +
                        " | End (Exception: " + e.getMessage() + ")" +
                        " | Duration: " + duration + "ms");
                throw e;
            }
        }
    }

    private TestService enhancedService;
    private ByteArrayOutputStream outputCapture;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() throws Exception {
        try {
            // Try INJECTION first (fastest and most compatible)
            Class<? extends TestService> dynamicType = new ByteBuddy()
                    .subclass(TestService.class)
                    .method(ElementMatchers.isAnnotatedWith(Monitored.class))
                    .intercept(MethodDelegation.to(MethodInterceptor.class))
                    .make()
                    .load(ByteBuddyInterceptorTest.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();

            enhancedService = dynamicType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Fallback to CHILD_FIRST if INJECTION fails
            try {
                Class<? extends TestService> dynamicType = new ByteBuddy()
                        .subclass(TestService.class)
                        .method(ElementMatchers.isAnnotatedWith(Monitored.class))
                        .intercept(MethodDelegation.to(MethodInterceptor.class))
                        .make()
                        .load(ByteBuddyInterceptorTest.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                        .getLoaded();

                enhancedService = dynamicType.getDeclaredConstructor().newInstance();
            } catch (Exception e2) {
                // Final fallback to creating a temporary class loader
                ClassLoader tempClassLoader = new ClassLoader(ByteBuddyInterceptorTest.class.getClassLoader()) {};

                Class<? extends TestService> dynamicType = new ByteBuddy()
                        .subclass(TestService.class)
                        .method(ElementMatchers.isAnnotatedWith(Monitored.class))
                        .intercept(MethodDelegation.to(MethodInterceptor.class))
                        .make()
                        .load(tempClassLoader)
                        .getLoaded();

                enhancedService = dynamicType.getDeclaredConstructor().newInstance();
            }
        }

        // Setup output capture for testing console output
        outputCapture = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputCapture));
    }

    @Test
    @DisplayName("Should intercept methods with @Monitored annotation")
    void testMonitoredMethodInterception() {
        // Test method with return value
        String result = enhancedService.processData("Hello", 5);

        assertEquals("Processed: Hello x5", result);

        String output = outputCapture.toString();
        assertTrue(output.contains("Method: processData | Parameters: [Hello, 5] | Start"));
        assertTrue(output.contains("Method: processData | Parameters: [Hello, 5] | End"));
    }

    @Test
    @DisplayName("Should intercept void methods with @Monitored annotation")
    void testMonitoredVoidMethodInterception() {
        enhancedService.calculateSomething(42.5, "square root");

        String output = outputCapture.toString();
        assertTrue(output.contains("Method: calculateSomething | Parameters: [42.5, square root] | Start"));
        assertTrue(output.contains("Method: calculateSomething | Parameters: [42.5, square root] | End"));
        assertTrue(output.contains("Calculating square root for value: 42.5"));
    }

    @Test
    @DisplayName("Should NOT intercept methods without @Monitored annotation")
    void testNonMonitoredMethodsNotIntercepted() {
        enhancedService.doSomething("test message");
        String result = enhancedService.regularMethod();

        assertEquals("This is a regular method", result);

        String output = outputCapture.toString();
        // Should not contain interception logs
        assertFalse(output.contains("Method: doSomething"));
        assertFalse(output.contains("Method: regularMethod"));
        // But should contain the actual method output
        assertTrue(output.contains("Doing: test message (not monitored)"));
    }

    @Test
    @DisplayName("Should intercept methods that return primitive types")
    void testPrimitiveReturnTypeInterception() {
        int result = enhancedService.addNumbers(10, 20);

        assertEquals(30, result);

        String output = outputCapture.toString();
        assertTrue(output.contains("Method: addNumbers | Parameters: [10, 20] | Start"));
        assertTrue(output.contains("Method: addNumbers | Parameters: [10, 20] | End"));
    }

    @Test
    @DisplayName("Should handle exceptions in intercepted methods")
    void testExceptionHandlingInInterception() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            enhancedService.throwException();
        });

        assertEquals("Test exception", exception.getMessage());

        String output = outputCapture.toString();
        assertTrue(output.contains("Method: throwException | Parameters: [] | Start"));
        assertTrue(output.contains("Method: throwException | Parameters: [] | End (Exception: Test exception)"));
    }

    @Test
    @DisplayName("Should measure execution duration")
    void testExecutionDurationMeasurement() {
        enhancedService.processData("test", 1);

        String output = outputCapture.toString();
        assertTrue(output.contains("Duration:"));
        assertTrue(output.contains("ms"));
    }

    @Test
    @DisplayName("Multiple method calls should be independently tracked")
    void testMultipleMethodCalls() {
        enhancedService.processData("first", 1);
        enhancedService.addNumbers(5, 3);
        enhancedService.doSomething("not monitored"); // This won't be intercepted

        String output = outputCapture.toString();

        // Should have interception logs for monitored methods
        assertTrue(output.contains("Method: processData | Parameters: [first, 1] | Start"));
        assertTrue(output.contains("Method: processData | Parameters: [first, 1] | End"));
        assertTrue(output.contains("Method: addNumbers | Parameters: [5, 3] | Start"));
        assertTrue(output.contains("Method: addNumbers | Parameters: [5, 3] | End"));

        // Should not have interception logs for non-monitored method
        assertFalse(output.contains("Method: doSomething"));
        assertTrue(output.contains("Doing: not monitored (not monitored)"));
    }

    @Test
    @DisplayName("Should work with different parameter types")
    void testDifferentParameterTypes() {
        enhancedService.calculateSomething(3.14159, "pi");
        enhancedService.addNumbers(-5, 10);
        enhancedService.processData("", 0);

        String output = outputCapture.toString();

        assertTrue(output.contains("Parameters: [3.14159, pi]"));
        assertTrue(output.contains("Parameters: [-5, 10]"));
        assertTrue(output.contains("Parameters: [, 0]"));
    }

    // Restore original System.out after each test
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // Integration test demonstrating real usage
    @Test
    @DisplayName("Integration test - Full workflow")
    void testFullWorkflow() {
        System.setOut(originalOut); // Use real output for this demo

        System.out.println("=== ByteBuddy Annotation-based Interception Demo ===");

        // Test various methods
        String result1 = enhancedService.processData("Integration Test", 3);
        System.out.println("Result: " + result1);

        int result2 = enhancedService.addNumbers(100, 200);
        System.out.println("Sum: " + result2);

        enhancedService.doSomething("This won't be intercepted");

        String result3 = enhancedService.regularMethod();
        System.out.println("Regular result: " + result3);

        System.out.println("=== Demo Complete ===");

        // Assertions
        assertEquals("Processed: Integration Test x3", result1);
        assertEquals(300, result2);
        assertEquals("This is a regular method", result3);
    }
}