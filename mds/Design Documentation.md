# Visual Flow Logger (VFL) System Documentation

## System Overview

Visual Flow Logger (VFL) is a hierarchical logging framework that structures application execution logs into tree-like hierarchies. Instead of traditional flat logging where events are recorded independently, VFL creates interconnected structures that represent the actual execution flow of your application.

The system represents program execution through two primary entities: **Blocks** (execution scopes) and **Logs** (chronological events), where blocks can contain other blocks, creating hierarchies that match program structure.

## Core VFL Concepts

### The Universal Block-Log Model

At its foundation, VFL operates on a universal dual-entity model that applies across all implementations:

**Blocks represent execution boundaries.** They define scopes of work - method calls, transactions, user sessions, or any logical grouping that makes sense for your application. Blocks establish the structural framework of your execution hierarchy.

**Logs represent events within those boundaries.** They capture what happened during execution - messages, warnings, errors, state changes, or any significant occurrences. Logs provide the chronological narrative within each block.

The relationship between these entities creates the hierarchy: blocks contain logs, and logs can reference other blocks, forming an interconnected structure that captures both temporal sequence and hierarchical structure.

### Universal Data Model

These core data structures are consistent across all VFL implementations:

#### Block Entity

```java
public class Block {
    private String id;              // Unique identifier across entire system
    private String parentBlockId;   // Creates tree structure (null for root)
    private String blockName;       // Human-readable description
}
```

#### Log Entity

```java
public class Log {
    private String id;              // Unique log identifier
    private String blockId;         // Which block owns this log
    private String parentLogId;     // Previous log in sequence
    private LogType logType;        // Classification of log entry
    private String message;         // Actual log content  
    private long timestamp;         // Precise timing information
}
```

#### SubBlockStartLog - The Universal Hierarchy Bridge

```java
public class SubBlockStartLog extends Log {
    private String referencedBlockId;  // The child block being initiated
}
```

This specialized log type creates connections between parent and child blocks across all VFL implementations, enabling the hierarchical structure through dual-purpose functionality: serving as both a regular log entry and a structural connector.

### Universal Hierarchical Relationship Mechanics

All VFL implementations use the same three-level linking mechanism:

1. **Parent-Child Block Relationships** - Child block's `parentBlockId` points to parent's `id`
2. **Sequential Log Relationships** - Logs within blocks linked via `parentLogId` references
3. **Cross-Hierarchy Connections** - `SubBlockStartLog` entries bridge parent log sequences to child blocks

### Universal Flow Types

VFL defines standard flow types that all implementations can represent:

- **SUB_BLOCK_START_PRIMARY**: Sequential execution where child completes before parent continues
- **SUB_BLOCK_START_SECONDARY_JOIN**: Parallel execution that will be explicitly joined
- **SUB_BLOCK_START_SECONDARY_NO_JOIN**: Fire-and-forget parallel execution
- **PUBLISH_EVENT**: Event publishing in event-driven architectures
- **EVENT_LISTENER**: Event listener activation

### Universal Buffer Interface

All VFL implementations use a consistent buffer interface for data ingestion:

```java
public interface VFLBuffer {
    void pushLogToBuffer(Log log);
    void pushBlockToBuffer(Block block); 
    void pushLogStartToBuffer(String blockId, long timestamp);
    void pushLogEndToBuffer(String blockId, BlockEndData endData);
    void flushAndClose();
}
```

The buffer system is implementation-agnostic and can be synchronous, asynchronous, batched, in-memory, networked, or hybrid depending on requirements.

## VFL Buffer System Architecture

The VFL system provides a flexible buffering architecture that handles data collection and output through a unified interface. The buffer system operates on a producer-consumer model where VFL operations produce data and flush handlers consume it.

### Buffer Base Classes

#### VFLBufferWithFlushHandlerBase

This abstract base class provides core buffering functionality that all VFL buffers extend:

```java
public abstract class VFLBufferWithFlushHandlerBase implements VFLBuffer {
    protected final int bufferSize;
    protected final VFLFlushHandler flushHandler;
    
    // Thread-safe storage for buffered data
    private final List<Log> logBuffer = Collections.synchronizedList(new ArrayList<>());
    private final List<Block> blockBuffer = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Long> blockStartBuffer = new ConcurrentHashMap<>();
    private final Map<String, BlockEndData> blockEndBuffer = new ConcurrentHashMap<>();
}
```

**Core Buffer Operations:**

- **Data Collection**: Thread-safe storage of blocks, logs, start times, and end data
- **Buffer Management**: Automatic flushing when buffer size limits are reached
- **Ordered Flushing**: Ensures data is sent to flush handlers in the correct order (blocks → starts → logs → ends)

**Flush Coordination:**

```java
protected final void performOrderedFlush(List<Log> logs, List<Block> blocks, 
                                       Map<String, Long> blockStarts, 
                                       Map<String, BlockEndData> blockEnds) {
    // Ordered execution ensures data dependencies are respected
    flushHandler.pushBlocksToServer(blocks);
    flushHandler.pushBlockStartsToServer(blockStarts);  
    flushHandler.pushLogsToServer(logs);
    flushHandler.pushBlockEndsToServer(blockEnds);
}
```

### Buffer Implementations

#### SynchronousBuffer

The simplest buffer implementation that processes data immediately:

```java
public class SynchronousBuffer extends VFLBufferWithFlushHandlerBase {
    @Override
    protected void executeFlushAll(List<Log> logs, List<Block> blocks, 
                                 Map<String, Long> blockStarts, 
                                 Map<String, BlockEndData> blockEnds) {
        // Execute synchronously - directly call the ordered flush
        performOrderedFlush(logs, blocks, blockStarts, blockEnds);
    }
}
```

**Characteristics:**
- No threading complexity
- Immediate data processing
- Minimal resource usage
- Suitable for development, testing, and low-throughput scenarios

#### AsyncBuffer

A sophisticated asynchronous buffer with periodic flushing and graceful shutdown:

```java
public class AsyncBuffer extends VFLBufferWithFlushHandlerBase {
    private final ExecutorService flushExecutor;           // Handles flush operations
    private final ScheduledExecutorService periodicExecutor; // Manages periodic flushing
    private final int flushTimeout;                         // Shutdown timeout
}
```

**Advanced Features:**

**Periodic Flushing:**
```java
// Automatic background flushing at regular intervals
periodicExecutor.scheduleWithFixedDelay(this::flushAll, 
    periodicFlushTimeMillisecond, periodicFlushTimeMillisecond, TimeUnit.MILLISECONDS);
```

**Resilient Execution:**
```java
@Override
protected void executeFlushAll(/*...*/) {
    if (flushExecutor.isShutdown()) {
        log.warn("Executor is shutdown, performing synchronous flush");
        performOrderedFlush(logs, blocks, blockStarts, blockEnds);
        return;
    }
    
    try {
        flushExecutor.submit(() -> performOrderedFlush(logs, blocks, blockStarts, blockEnds));
    } catch (RejectedExecutionException e) {
        log.warn("Task rejected by executor, performing synchronous flush", e);
        performOrderedFlush(logs, blocks, blockStarts, blockEnds);
    }
}
```

**Graceful Shutdown:**
```java
@Override
public void flushAndClose() {
    // 1. Stop periodic flushing
    periodicExecutor.shutdown();
    
    // 2. Flush remaining data
    super.flushAll();
    
    // 3. Shutdown flush executor with timeout
    flushExecutor.shutdown();
    
    // 4. Wait for completion with timeout handling
    if (!flushExecutor.awaitTermination(flushTimeout, TimeUnit.MILLISECONDS)) {
        flushExecutor.shutdownNow();
        throw new RuntimeException("Flush timeout exceeded: " + flushTimeout + "ms");
    }
    
    // 5. Close flush handler
    flushHandler.closeFlushHandler();
}
```

### Flush Handler System

#### VFLFlushHandler Interface

Defines how buffered data is ultimately processed and output:

```java
public interface VFLFlushHandler {
    boolean pushLogsToServer(List<Log> logs);
    boolean pushBlocksToServer(List<Block> blocks);
    boolean pushBlockStartsToServer(Map<String, Long> blockStarts);
    boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds);
    void closeFlushHandler();
}
```

#### NestedJsonFlushHandler Implementation

A comprehensive flush handler that generates hierarchical JSON output:

```java
public class NestedJsonFlushHandler implements VFLFlushHandler {
    // Thread-safe data storage
    private final Map<String, Block> blocks = new ConcurrentHashMap<>();
    private final Map<String, Log> logs = new ConcurrentHashMap<>();
    private final Map<String, Long> blockStarts = new ConcurrentHashMap<>();
    private final Map<String, BlockEndData> blockEnds = new ConcurrentHashMap<>();
}
```

**Hierarchical Structure Building:**

```java
private List<BlockJson> buildNestedStructure() {
    // Find root blocks (blocks with no parent)
    List<Block> rootBlocksList = blocks.values().stream()
        .filter(block -> block.getParentBlockId() == null)
        .toList();
    
    List<BlockJson> result = new ArrayList<>();
    for (Block rootBlock : rootBlocksList) {
        result.add(buildBlockJson(rootBlock));
    }
    return result;
}
```

**Recursive Log Chain Construction:**

```java
private List<LogJson> buildLogsChain(String blockId, String parentLogId) {
    // Get logs for this block with specified parent
    List<Log> blockLogs = logs.values().stream()
        .filter(log -> Objects.equals(log.getBlockId(), blockId))
        .filter(log -> Objects.equals(log.getParentLogId(), parentLogId))
        .sorted(Comparator.comparing(Log::getTimestamp))
        .toList();
    
    // Process each log and build nested structures
    for (Log log : blockLogs) {
        // Handle SubBlockStartLog special case with referenced blocks
        if (log instanceof SubBlockStartLog subBlockLog) {
            // Add referenced block and timing information
            logJson.referencedBlock = buildBlockJson(referencedBlock);
        }
        
        // Recursively build nested log chains
        List<LogJson> nestedLogs = buildLogsChain(blockId, log.getId());
        if (!nestedLogs.isEmpty()) {
            logJson.logsChain = nestedLogs;
        }
    }
}
```

**Duration and Timing Calculations:**

```java
private String formatDuration(long durationMillis) {
    if (durationMillis < 1000) {
        return durationMillis + "ms";
    } else if (durationMillis < 60000) {
        double seconds = durationMillis / 1000.0;
        return String.format("%.3fs", seconds);
    } else {
        long minutes = durationMillis / 60000;
        long remainingMs = durationMillis % 60000;
        double remainingSeconds = remainingMs / 1000.0;
        return String.format("%dmin %.3fs", minutes, remainingSeconds);
    }
}
```

## Current Implementation: Annotation-Based Approach

The annotation-based implementation provides automatic, transparent VFL integration through runtime instrumentation.

### Annotation-Based Components

#### @VFLBlock Annotation

```java
@VFLBlock(blockName = "Processing order {0}")
public Order processOrder(String orderId) {
    // Method implementation
}
```

#### VFLAnnotationProcessor

Uses ByteBuddy for runtime bytecode transformation, automatically adding VFL hooks to annotated methods.

#### ContextManager - Core Context Management

The ContextManager serves as the central coordination point for the annotation-based approach:

```java
public class ContextManager {
    // Thread-local storage for context stacks
    public static final ThreadLocal<Stack<VFLBlockContext>> loggerCtxStack = new ThreadLocal<>();
    public static final ThreadLocal<SpawnedThreadContext> spawnedThreadContext = new ThreadLocal<>();
    
    // Global buffer reference
    public static VFLBuffer AnnotationBuffer;
}
```

**Core Context Operations:**

**Context Initialization:**
```java
private static void initializeContextStack() {
    loggerCtxStack.set(new Stack<>());
}

private static void pushContext(VFLBlockContext context) {
    if (loggerCtxStack.get() == null) {
        initializeContextStack();
    }
    loggerCtxStack.get().push(context);
}
```

**Root Block Management:**
```java
public static void startRootBlock(String blockName) {
    Block rootBlock = CreateBlockAndPush2Buffer(blockName, null, AnnotationBuffer);
    VFLBlockContext rootContext = new VFLBlockContext(rootBlock, AnnotationBuffer);
    
    initializeContextStack();
    pushContext(rootContext);
    spawnedThreadContext.remove(); // Clean up any leftover context
    logger.ensureBlockStarted();
}
```

**Sub-Block Creation:**
```java
public static void startSubBlock(String blockName) {
    VFLBlockContext parentContext = getCurrentContext();
    
    // Create child block with parent relationship
    Block primarySubBlockStart = CreateBlockAndPush2Buffer(
        blockName, parentContext.blockInfo.getId(), AnnotationBuffer
    );
    
    // Create linking log in parent's sequence
    SubBlockStartLog subBlockStartLog = CreateLogAndPush2Buffer(
        parentContext.blockInfo.getId(),
        parentContext.currentLogId,
        null,
        primarySubBlockStart.getId(),
        LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
        AnnotationBuffer
    );
    
    // Update parent's log sequence
    parentContext.currentLogId = subBlockStartLog.getId();
    
    // Create and push new context
    VFLBlockContext currentContext = new VFLBlockContext(primarySubBlockStart, AnnotationBuffer);
    pushContext(currentContext);
    logger.ensureBlockStarted();
}
```

**Context Cleanup:**
```java
public static void closeCurrentContext(Object returnValue) {
    // Close the logger context
    logger.close("Returning " + returnValue);
    
    // Pop the context
    Stack<VFLBlockContext> stack = loggerCtxStack.get();
    if (stack != null && !stack.isEmpty()) {
        VFLBlockContext poppedContext = stack.pop();
        
        // Clean up if stack is empty
        if (stack.isEmpty()) {
            cleanupThreadContext(poppedContext);
        }
    }
}

private static void cleanupThreadContext(VFLBlockContext lastContext) {
    boolean isRootThread = !isSpawnedThread();
    
    if (isRootThread) {
        // Trigger final flush and close
        AnnotationBuffer.flushAndClose();
    }
    
    // Clean up thread-local resources
    loggerCtxStack.remove();
    spawnedThreadContext.remove();
}
```

### Advanced Cross-Thread Propagation: VFLFutures

VFLFutures provides sophisticated cross-thread context propagation for asynchronous operations:

#### SpawnedThreadContext Record

```java
public record SpawnedThreadContext(
    VFLBlockContext parentContext,        // Original thread's context
    LogTypeBlockStartEnum startType       // Relationship type (JOIN vs NO_JOIN)
)
```

#### Context Wrapping Mechanisms

**Supplier Wrapping:**
```java
private static <R> Supplier<R> wrapSupplier(Supplier<R> supplier) {
    var spawnedThreadCtx = createSpawnedThreadContext();
    return () -> {
        try {
            setupSpawnedThreadContext(spawnedThreadCtx);
            return supplier.get();
        } finally {
            // Manual cleanup for lambda-created contexts
            // CM only manages @SubBlock annotated methods
            loggerCtxStack.remove();
            spawnedThreadContext.remove();
        }
    };
}
```

**Runnable Wrapping:**
```java
private static Runnable wrapRunnable(Runnable runnable) {
    var spawnedThreadCtx = createSpawnedThreadContext();
    return () -> {
        try {
            setupSpawnedThreadContext(spawnedThreadCtx);
            runnable.run();
        } finally {
            // Manual cleanup for lambda-created contexts
            loggerCtxStack.remove();
            spawnedThreadContext.remove();
        }
    };
}
```

#### Context Setup Process

```java
private static void setupSpawnedThreadContext(SpawnedThreadContext spawnedThreadContext) {
    if (!VFLAnnotationProcessor.initialized) return;
    
    // Validate context state
    var existingCtx = ContextManager.spawnedThreadContext.get();
    if (existingCtx != null) {
        log.warn("Spawned Thread Context is not null! {}", GetThreadInfo());
    }
    
    // Set the spawned context for the new thread
    ContextManager.spawnedThreadContext.set(spawnedThreadContext);
}
```

#### Public API Methods

```java
public class VFLFutures {
    // Supplier-based async operations
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier)
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor)
    
    // Runnable-based async operations  
    public static CompletableFuture<Void> runAsync(Runnable runnable)
    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)
}
```

#### Spawned Thread Block Creation

When a spawned thread begins logging without an active @VFLBlock context:

```java
public static void startSubBlockFromSpawnedThreadContext(String blockName) {
    SpawnedThreadContext callerData = spawnedThreadContext.get();
    
    // Create sub block in new thread with parent relationship
    Block subBlockNewThread = CreateBlockAndPush2Buffer(
        blockName,
        callerData.parentContext().blockInfo.getId(),
        AnnotationBuffer
    );
    
    // Create linking log in parent thread's sequence
    CreateLogAndPush2Buffer(
        callerData.parentContext().blockInfo.getId(),
        callerData.parentContext().currentLogId,
        null,
        subBlockNewThread.getId(),
        callerData.startType(), // SUB_BLOCK_START_SECONDARY_JOIN
        AnnotationBuffer
    );
    
    // Initialize new thread's context stack
    VFLBlockContext currentContext = new VFLBlockContext(subBlockNewThread, AnnotationBuffer);
    initializeContextStack();
    pushContext(currentContext);
    logger.ensureBlockStarted();
}
```

### Annotation-Based VFL Core Engine

The annotation implementation provides a concrete VFL engine implementation:

```java
public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);
    
    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            final VFLBlockContext context = getContext();
            final long startTimestamp = Instant.now().toEpochMilli();
            context.buffer.pushLogStartToBuffer(context.blockInfo.getId(), startTimestamp);
        }
    }
    
    public final void info(String message) {
        logInternal(LogTypeEnum.MESSAGE, message);
    }
    
    protected abstract VFLBlockContext getContext();
}
```

**Annotation-Specific Logger Implementation:**

```java
public static VFL logger = new VFL() {
    @Override
    protected VFLBlockContext getContext() {
        // Auto-create spawned thread blocks when needed
        if (!hasActiveContext() && isSpawnedThread()) {
            startSubBlockFromSpawnedThreadContext(
                Thread.currentThread().getName() + "_" + Thread.currentThread().getId()
            );
        }
        return getCurrentContext();
    }
};
```

### Static Log API

Provides convenient static methods for the annotation approach:

```java
public class Log {
    public static void Info(String message, Object... args)
    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer)
    // Other logging methods
}
```

### How the Annotation Approach Works

#### Runtime Instrumentation Process

1. ByteBuddy agent installs at application startup
2. Classes with `@VFLBlock` methods are identified and transformed
3. Method entry/exit advice is injected into annotated methods
4. Original method behavior remains unchanged

#### Thread-Local Context Management

1. Each thread maintains its own context stack
2. Annotated method entry creates/pushes block context
3. Method exit pops context and closes block
4. Cross-thread propagation handled through spawned contexts

#### Automatic Hierarchy Creation

1. First annotated method on thread creates root block
2. Nested annotated methods create child blocks
3. Parent-child relationships established automatically
4. Log sequencing maintained within each block context

### Annotation-Based Error Handling

The annotation implementation follows VFL's non-intrusive error handling principle:

```pseudocode
function onMethodExit(method, args, returnValue, thrownException):
    if thrownException != null:
        // Log exception in current block context
        ContextManager.logException(thrownException)
    
    // Always close current context
    ContextManager.closeCurrentContext(returnValue)
    
    // Never interfere with exception propagation
    if thrownException != null:
        rethrow thrownException
```

**Graceful Degradation in Annotation Approach:**

- If VFL initialization fails, static methods become no-ops
- Buffer failures don't block application execution
- Context corruption triggers cleanup and continuation
- Thread-local cleanup prevents memory leaks

### Annotation-Based Usage Patterns

**Simple Linear Flow:**

```java
@VFLBlock
public void processOrder() {
    Log.Info("Starting order validation");
    validateOrder();  // Creates sub-block
    Log.Info("Order validated, processing payment");
    processPayment(); // Creates another sub-block
}
```

**Parallel Execution:**

```java
@VFLBlock
public void processDataAsync() {
    Log.Info("Starting parallel processing");
    
    var task1 = VFLFutures.supplyAsync(() -> processChunk1());
    var task2 = VFLFutures.supplyAsync(() -> processChunk2());
    
    var result1 = task1.join();
    var result2 = task2.join();
    
    Log.Info("Parallel processing completed");
}
```

## Future Implementation Approaches

These approaches will use the same core VFL data model and buffer system but provide different integration methods:

### Context-Passing Approach (Planned)

Explicit context objects passed through application calls, providing full control over block boundaries and relationships.

### Scoped Logger Approach (Planned)

Try-with-resources pattern for automatic scope management, enabling clean resource handling and guaranteed cleanup.

### Fluent API Approach (Planned)

Method chaining for readable logging sequences, allowing expressive and intuitive VFL usage.

## Summary

VFL is a universal hierarchical logging framework built around core Block and Log entities. The annotation-based implementation described in this document is one approach to integrating VFL into applications.

Key architectural components:

- **Universal Data Model**: Block and Log entities with hierarchical relationships
- **Flexible Buffer System**: Synchronous and asynchronous buffer implementations with pluggable flush handlers
- **Annotation-Based Integration**: Runtime instrumentation with automatic context management
- **Cross-Thread Propagation**: Proper async operation support through VFLFutures
- **Output Flexibility**: JSON flush handler with hierarchical structure generation for development and testing or Flush to VFL Hub for centralized storage for distributed systems

The annotation approach provides transparent, automatic VFL integration through runtime instrumentation, while the buffer system ensures efficient data handling and flexible output options. Future implementations will provide alternative integration methods while maintaining the same core data model, buffer system, and hierarchical concepts.