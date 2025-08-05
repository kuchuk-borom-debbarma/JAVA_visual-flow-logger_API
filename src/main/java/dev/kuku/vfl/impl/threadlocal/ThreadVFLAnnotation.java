package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Stack;

public class ThreadVFLAnnotation {

    static VFLBuffer buffer;
    private static boolean initialized = false;
    // If a sub block is started in a new thread this variable will store the started sub block
    private static ThreadLocal<Block> startedSubBlockInParentThread = new ThreadLocal<>();

    public static synchronized void initialise(VFLBuffer buffer) {
        if (initialized) {
            System.out.println("VFL already initialized, skipping...");
            return;
        }

        try {
            // Install ByteBuddy agent
            Instrumentation instrumentation = ByteBuddyAgent.install();
            ThreadVFLAnnotation.buffer = buffer;

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
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder.visit(Advice.to(ThreadVFLAnnotation.class)
                            .on(ElementMatchers.isAnnotatedWith(VFLBlock.class))))

                    // Install on the instrumentation
                    .installOn(instrumentation);

            initialized = true;
            System.out.println("VFL instrumentation initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize VFL instrumentation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("VFL initialization failed", e);
        }
    }

    /**
     * Reset initialization state (useful for testing)
     */
    public static synchronized void reset() {
        initialized = false;
        ThreadVFLAnnotation.buffer = null;
    }

    private static String getMethodName(Method method, Object[] args) {
        VFLBlock vflBlock = method.getAnnotation(VFLBlock.class);
        // If VFLBlock has a non-empty blockName, use it as a template
        if (vflBlock != null && !vflBlock.blockName().isEmpty()) {
            String template = vflBlock.blockName();

            // Replace placeholders {0}, {1}, etc. with argument string representations
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    String placeholder = "{" + i + "}";
                    String argString = args[i] != null ? args[i].toString() : "null";
                    template = template.replace(placeholder, argString);
                }
            }

            return template;
        }

        // Fallback to original behavior if no VFLBlock or empty blockName
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i] != null ? args[i].toString() : "null");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    @Advice.OnMethodEnter
    static void onEnter(@Advice.Origin Method method,
                        @Advice.AllArguments Object[] args) {
        String blockName = getMethodName(method, args);

        var loggerStack = ThreadVFL.LOGGER_STACK.get();
        if (loggerStack == null) {
            //if logger stack is null it can be one of two things
            //1. Start of a new operation if startedSubBlockInParentThread is null
            //2. Sub block start in a new thread if startedSubBlockInParentThread is valid

            //Scenario 2
            if (startedSubBlockInParentThread.get() != null) {
                //Create a new stack and set it as this thread's logger stack by pushing startedSubBlockInParent to the stack
                Stack<ThreadVFL> stack = new Stack<>();
                stack.push(new ThreadVFL(new VFLBlockContext(startedSubBlockInParentThread.get(), buffer)));
                return;
            }
            //Scenario 1
            //Create root block and push it to a new stack
            Block rootBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, null, buffer);
            ThreadVFL rootLogger = new ThreadVFL(new VFLBlockContext(rootBlock, buffer));
            loggerStack = new Stack<>();
            loggerStack.push(rootLogger);
        } else {
            //If logger stack is valid it's already part of an on-going flow and this method invocation is a sub block start
            VFLBlockContext parentBlock = ThreadVFL.getCurrentLogger().loggerContext;
            Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, parentBlock.blockInfo.getId(), buffer);
            SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                    parentBlock.blockInfo.getId(),
                    parentBlock.currentLogId,
                    null,
                    subBlock.getId(),
                    LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY, buffer);
            //Update flow
            parentBlock.currentLogId = subBlockStartLog.getId();
        }
    }

    @Advice.OnMethodExit
    static void onExit() {
        //This will close the block and pop the logger from thread and remove the stack if it's empty.
        ThreadVFL.getCurrentLogger().onClose(null);
        if (ThreadVFL.LOGGER_STACK.get() == null) {
            //If startedSubBlockInParentThread is null then it's root logger and this marks the end of the operation
            if (startedSubBlockInParentThread.get() == null) {
                buffer.flushAndClose();
            }
            //Remove the startedSubBlockInParentThread IN CASE this is a spawned thread
            startedSubBlockInParentThread.remove();
        }
    }
}