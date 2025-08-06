<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

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

## VFL Implementation Approaches

VFL provides multiple integration approaches to fit different coding styles and requirements. Each approach uses the same core data model and buffer system but provides different ways to integrate VFL into your application.

### Current Implementation: Annotation-Based Approach

The annotation-based implementation is one approach that provides automatic, transparent VFL integration through runtime instrumentation.

#### Annotation-Based Components

**@VFLBlock Annotation:**

```java
@VFLBlock(blockName = "Processing order {0}")
public Order processOrder(String orderId) {
    // Method implementation
}
```

**VFLAnnotationProcessor:**
Uses ByteBuddy for runtime bytecode transformation, automatically adding VFL hooks to annotated methods.

**ContextManager:**
Manages thread-local context stacks specific to the annotation approach:

```java
public class ContextManager {
    public static final ThreadLocal<Stack<VFLBlockContext>> loggerCtxStack = new ThreadLocal<>();
    public static final ThreadLocal<SpawnedThreadContext> spawnedThreadContext = new ThreadLocal<>();
    public static VFLBuffer AnnotationBuffer;
}
```

**VFLAnnotationAdvice:**
Bytecode advice that intercepts method entry/exit for annotated methods:

```java
public class VFLAnnotationAdvice {
    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] args)
    
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(/* parameters */)
}
```

**Static Log API:**
Provides convenient static methods for the annotation approach:

```java
public class Log {
    public static void Info(String message, Object... args)
    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer)
    // Other logging methods
}
```


#### How the Annotation Approach Works

**Runtime Instrumentation Process:**

1. ByteBuddy agent installs at application startup
2. Classes with `@VFLBlock` methods are identified and transformed
3. Method entry/exit advice is injected into annotated methods
4. Original method behavior remains unchanged

**Thread-Local Context Management:**

1. Each thread maintains its own context stack
2. Annotated method entry creates/pushes block context
3. Method exit pops context and closes block
4. Cross-thread propagation handled through spawned contexts

**Automatic Hierarchy Creation:**

1. First annotated method on thread creates root block
2. Nested annotated methods create child blocks
3. Parent-child relationships established automatically
4. Log sequencing maintained within each block context

### Future Implementation Approaches

These approaches will use the same core VFL data model and buffer system but provide different integration methods:

#### Context-Passing Approach (Planned)

Explicit context objects passed through application calls, providing full control over block boundaries and relationships.

#### Scoped Logger Approach (Planned)

Try-with-resources pattern for automatic scope management, enabling clean resource handling and guaranteed cleanup.

#### Fluent API Approach (Planned)

Method chaining for readable logging sequences, allowing expressive and intuitive VFL usage.

## Annotation-Based Implementation Details

*Note: The following sections describe the specific mechanics of the current annotation-based implementation, not universal VFL concepts.*

### Annotation-Based Context Architecture

The annotation implementation uses thread-local context stacks to maintain execution hierarchy:

```java
public class VFLBlockContext {
    public final Block blockInfo;              // Current block metadata
    public final AtomicBoolean blockStarted;   // Lazy initialization flag
    public final VFLBuffer buffer;             // Data output destination
    public String currentLogId;                // Last log in sequence
}
```

**Stack Operations in Annotation Approach:**

**Context Initialization:**

```pseudocode
function initializeContextStack():
    threadLocalStack = new Stack<VFLBlockContext>()
    setThreadLocal(threadLocalStack)
```

**Context Pushing (Method Entry):**

```pseudocode
function startSubBlock(blockName):
    parentContext = getCurrentContext()
    
    // Create child block with parent relationship
    childBlock = createBlock(blockName, parentContext.blockInfo.id)
    
    // Create linking log in parent's sequence
    linkingLog = createSubBlockStartLog(
        parentBlockId = parentContext.blockInfo.id,
        parentLogId = parentContext.currentLogId,
        referencedBlockId = childBlock.id
    )
    
    // Update parent's log sequence
    parentContext.currentLogId = linkingLog.id
    
    // Push new context for child block
    childContext = new VFLBlockContext(childBlock, buffer)
    threadLocalStack.push(childContext)
```

**Context Popping (Method Exit):**

```pseudocode
function closeCurrentContext(returnValue):
    currentContext = getCurrentContext()
    closeBlock(currentContext, returnValue)
    poppedContext = threadLocalStack.pop()
    
    if threadLocalStack.isEmpty():
        cleanupThreadResources()
```


### Annotation-Based Cross-Thread Propagation

The annotation implementation handles asynchronous operations through spawned thread contexts:

```java
public record SpawnedThreadContext(
    VFLBlockContext parentContext,        // Original thread's context
    LogTypeBlockStartEnum startType       // Relationship type
)
```

**Async Operation Wrapping:**

```pseudocode
function createAsyncOperation(supplier):
    currentContext = getCurrentContext()
    spawnedContext = new SpawnedThreadContext(currentContext, SUB_BLOCK_START_SECONDARY_JOIN)
    
    wrappedSupplier = () -> {
        setSpawnedThreadContext(spawnedContext)
        return supplier.get()
    }
    
    return CompletableFuture.supplyAsync(wrappedSupplier)
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
        return ContextManager.getCurrentContext();
    }
};
```


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


## Summary

VFL is a universal hierarchical logging framework built around core Block and Log entities. The annotation-based implementation described in this document is one approach to integrating VFL into applications. Future implementations will provide alternative integration methods (context-passing, scoped logger, fluent API) while maintaining the same core data model, buffer system, and hierarchical concepts.

The annotation approach provides transparent, automatic VFL integration through runtime instrumentation, while other approaches will offer different trade-offs in terms of control, explicitness, and integration complexity.

