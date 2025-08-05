package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class ThreadVFLAnnotationInitializer {

    private static boolean initialized = false;

    public static synchronized void initialise(VFLBuffer buffer) {
        if (initialized) {
            System.out.println("VFL already initialized, skipping...");
            return;
        }

        try {
            // Install ByteBuddy agent
            Instrumentation instrumentation = ByteBuddyAgent.install();

            // Set the buffer in advice
            ThreadVFLAdvice.buffer = buffer;

            // Create the agent builder and apply transformations
            new AgentBuilder.Default()
                    // Enable retransformation for already loaded classes
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)

                    // Add listener for debugging (optional)
                    .with(new AgentBuilder.Listener() {
                        @Override
                        public void onDiscovery(String typeName, ClassLoader classLoader,
                                                net.bytebuddy.utility.JavaModule module, boolean loaded) {
                            // Called when a type is discovered for potential transformation
                        }

                        @Override
                        public void onTransformation(net.bytebuddy.description.type.TypeDescription typeDescription,
                                                     ClassLoader classLoader,
                                                     net.bytebuddy.utility.JavaModule module,
                                                     boolean loaded,
                                                     net.bytebuddy.dynamic.DynamicType dynamicType) {
                            System.out.println("VFL: Transformed " + typeDescription.getName() + " (loaded: " + loaded + ")");
                        }

                        @Override
                        public void onIgnored(net.bytebuddy.description.type.TypeDescription typeDescription,
                                              ClassLoader classLoader,
                                              net.bytebuddy.utility.JavaModule module,
                                              boolean loaded) {
                            // Uncomment for verbose debugging
                            // System.out.println("VFL: Ignored " + typeDescription.getName());
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader,
                                            net.bytebuddy.utility.JavaModule module, boolean loaded, Throwable throwable) {
                            System.err.println("VFL: Error transforming " + typeName + ": " + throwable.getMessage());
                            throwable.printStackTrace();
                        }

                        @Override
                        public void onComplete(String typeName, ClassLoader classLoader,
                                               net.bytebuddy.utility.JavaModule module, boolean loaded) {
                            // Optional: completion callback
                        }
                    })

                    // Target all types that have methods annotated with @VFLBlock
                    .type(ElementMatchers.declaresMethod(
                            ElementMatchers.isAnnotatedWith(VFLBlock.class)
                    ))

                    // Transform methods annotated with @VFLBlock
                    .transform(new AgentBuilder.Transformer() {
                        @Override
                        public net.bytebuddy.dynamic.DynamicType.Builder<?> transform(
                                net.bytebuddy.dynamic.DynamicType.Builder<?> builder,
                                net.bytebuddy.description.type.TypeDescription typeDescription,
                                ClassLoader classLoader,
                                net.bytebuddy.utility.JavaModule module,
                                java.security.ProtectionDomain protectionDomain) {
                            return builder.visit(Advice.to(ThreadVFLAdvice.class)
                                    .on(ElementMatchers.isAnnotatedWith(VFLBlock.class)));
                        }
                    })

                    // Install on the instrumentation
                    .installOn(instrumentation);

            // Try to retransform already loaded classes that might have @VFLBlock annotations
            retransformLoadedClasses(instrumentation);

            initialized = true;
            System.out.println("VFL instrumentation initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize VFL instrumentation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("VFL initialization failed", e);
        }
    }

    private static void retransformLoadedClasses(Instrumentation instrumentation) {
        try {
            Class<?>[] loadedClasses = instrumentation.getAllLoadedClasses();
            int skipped = 0, processed = 0, transformed = 0;

            for (Class<?> clazz : loadedClasses) {
                try {
                    // Apply comprehensive filtering
                    if (shouldSkipClass(clazz)) {
                        skipped++;
                        continue;
                    }

                    if (!instrumentation.isModifiableClass(clazz)) {
                        skipped++;
                        continue;
                    }

                    processed++;
                    if (hasVFLBlockAnnotation(clazz)) {
                        System.out.println("VFL: Retransforming already loaded class: " + clazz.getName());
                        instrumentation.retransformClasses(clazz);
                        transformed++;
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    System.err.println("VFL: Failed to process class " + clazz.getName() + ": " + e.getMessage());
                    skipped++;
                }
            }

            System.out.println("VFL: Class processing summary - Processed: " + processed +
                    ", Transformed: " + transformed + ", Skipped: " + skipped);
        } catch (Exception e) {
            System.err.println("VFL: Error during retransformation: " + e.getMessage());
        }
    }

    private static boolean shouldSkipClass(Class<?> clazz) {
        String className = clazz.getName();

        // Skip JVM system classes
        if (className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("sun.") ||
                className.startsWith("com.sun.") ||
                className.startsWith("jdk.") ||
                className.startsWith("org.omg.") ||
                className.startsWith("org.w3c.") ||
                className.startsWith("org.xml.")) {
            return true;
        }

        // Skip common framework classes that might have dependency issues
        if (className.startsWith("org.springframework.") ||
                className.startsWith("org.hibernate.") ||
                className.startsWith("org.apache.") ||
                className.startsWith("com.google.") ||
                className.startsWith("io.netty.") ||
                className.startsWith("org.slf4j.") ||
                className.startsWith("ch.qos.logback.") ||
                className.startsWith("org.eclipse.") ||
                className.startsWith("org.junit.") ||
                className.startsWith("org.mockito.")) {
            return true;
        }

        // Skip ByteBuddy and instrumentation classes
        if (className.startsWith("net.bytebuddy.") ||
                className.startsWith("java.lang.instrument.")) {
            return true;
        }

        // Skip array classes and synthetic classes
        if (clazz.isArray() || clazz.isSynthetic()) {
            return true;
        }

        // Skip primitive classes
        if (clazz.isPrimitive()) {
            return true;
        }

        // Skip interfaces (they can't have method implementations anyway)
        if (clazz.isInterface()) {
            return true;
        }

        // Skip enums (unless you specifically want to transform them)
        if (clazz.isEnum()) {
            return true;
        }

        // Skip anonymous classes (they usually don't have annotations)
        if (clazz.isAnonymousClass()) {
            return true;
        }

        return false;
    }

    private static boolean hasVFLBlockAnnotation(Class<?> clazz) {
        try {
            // Additional safety check
            if (clazz.isInterface() || clazz.isAnnotation() || clazz.isPrimitive()) {
                return false;
            }

            return java.util.Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(method -> {
                        try {
                            return method.isAnnotationPresent(VFLBlock.class);
                        } catch (Exception | NoClassDefFoundError e) {
                            // Individual method check failed, continue with others
                            return false;
                        }
                    });
        } catch (Exception | NoClassDefFoundError e) {
            // Log the error for debugging but don't fail
            System.err.println("VFL: Cannot inspect methods of " + clazz.getName() +
                    " due to missing dependencies: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if VFL has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset initialization state (useful for testing)
     */
    public static synchronized void reset() {
        initialized = false;
        ThreadVFLAdvice.buffer = null;
    }
}