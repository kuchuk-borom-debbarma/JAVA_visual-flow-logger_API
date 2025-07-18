package dev.kuku.vfl.core.util;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class HelperUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }

    public static String getCurrentMethodName() {
        return MethodHandles.lookup().lookupClass().getEnclosingMethod().getName();
    }

    /**
     * Gets the method name from the calling context, skipping the specified number of stack frames
     * @param skipFrames number of frames to skip (0 = current method, 1 = caller, etc.)
     * @return method name from the stack trace
     */
    public static String getMethodNameFromStack(int skipFrames) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // stackTrace[0] is getStackTrace()
        // stackTrace[1] is this method
        // stackTrace[2] is the caller
        // We want to skip additional frames as specified
        int targetIndex = 2 + skipFrames;
        if (targetIndex < stackTrace.length) {
            String fullMethodName = stackTrace[targetIndex].getMethodName();
            // Handle lambda expressions - they often have names like "lambda$sum$0"
            if (fullMethodName.startsWith("lambda$")) {
                String[] parts = fullMethodName.split("\\$");
                if (parts.length >= 2) {
                    return parts[1]; // Return the actual method name part
                }
            }
            return fullMethodName;
        }
        return "unknown";
    }

    /**
     * Attempts to extract method name from lambda context
     * This looks further up the stack to find the originating method
     */
    public static String getLambdaOriginMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Look through the stack trace for lambda patterns
        for (int i = 2; i < stackTrace.length; i++) {
            String methodName = stackTrace[i].getMethodName();

            // If we find a lambda method, try to extract the original method name
            if (methodName.startsWith("lambda$")) {
                String[] parts = methodName.split("\\$");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }

            // If we find a method that's not a lambda and not part of the logging framework,
            // it's likely the originating method
            String className = stackTrace[i].getClassName();
            if (!className.startsWith("dev.kuku.vfl.") &&
                    !methodName.equals("run") &&
                    !methodName.equals("call") &&
                    !methodName.startsWith("lambda$")) {
                return methodName;
            }
        }

        return "unknown";
    }
}