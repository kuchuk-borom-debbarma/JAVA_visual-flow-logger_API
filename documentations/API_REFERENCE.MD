# VFL Java Client - API Reference

## Table of Contents

1. [VFLInitializer](#vflinitializer)
2. [VFLStarter](#vflstarter)
3. [Log](#log)
4. [VFLFutures](#vflfutures)
5. [@SubBlock Annotation](#subblock-annotation)
6. [Buffer Configuration](#buffer-configuration)
7. [Flush Handlers](#flush-handlers)
8. [Placeholder Patterns](#placeholder-patterns)
9. [Error Handling](#error-handling)
10. [Thread Safety](#thread-safety)

---

## VFLInitializer

Entry point for setting up VFL instrumentation. **Must be called before any application classes containing `@SubBlock` methods are loaded.**

### Methods

#### `initialize(VFLAnnotationConfig config)`

Initializes VFL annotation-based instrumentation with bytecode modification.

**Parameters:**
- `config` - Configuration object with buffer and enable/disable flag

**Example:**
```java
VFLBuffer buffer = new AsyncBuffer(100, 3000, 1000, flushHandler, executor, scheduler);
VFLAnnotationConfig config = new VFLAnnotationConfig(false, buffer);
VFLInitializer.initialize(config);
```

#### `isDisabled()`

Checks if VFL is currently disabled.

**Returns:** `boolean` - true if disabled or not initialized

**Thread Safety:** Thread-safe

---

## VFLStarter

Main entry point for creating VFL trace contexts. Provides different ways to start and continue traces.

### Root Block Methods

#### `StartRootBlock(String blockName, Runnable runnable)`

Start a new root-level trace block.

**Parameters:**
- `blockName` - Logical name for the operation
- `runnable` - Code to execute within trace context

**Example:**
```java
VFLStarter.StartRootBlock("Process Order", () -> {
    orderService.processOrder();
});
```

#### `StartRootBlock(String blockName, Supplier<R> supplier)`

Start a root block with return value.

**Parameters:**
- `blockName` - Logical name for the operation
- `supplier` - Code to execute that returns a value

**Returns:** `R` - Result from the supplier

**Example:**
```java
String result = VFLStarter.StartRootBlock("Calculate Total", () -> {
    return orderService.calculateTotal();
});
```

### Continuation Methods

#### `ContinueFromBlock(Block continuationBlock, Runnable runnable)`

Continue tracing from a block received from another service.

**Parameters:**
- `continuationBlock` - Block object from upstream service
- `runnable` - Code to execute in continued trace

**Example:**
```java
Block traceBlock = deserialize(request.getHeader("vfl-block"));
VFLStarter.ContinueFromBlock(traceBlock, () -> {
    handleRequest();
});
```

#### `ContinueFromBlock(Block continuationBlock, Supplier<R> supplier)`

Continue tracing with return value.

**Parameters:**
- `continuationBlock` - Block object from upstream service
- `supplier` - Code to execute that returns a value

**Returns:** `R` - Result from the supplier

### Event Listener Methods

#### `StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, Runnable runnable)`

Start tracing for an event listener/consumer.

**Parameters:**
- `publisherBlock` - Event publisher block linking to original trace
- `eventListenerName` - Logical name for this event listener
- `runnable` - Code to execute for event handling

**Example:**
```java
VFLStarter.StartEventListener(publisherBlock, "OrderEventListener", () -> {
    processOrderEvent();
});
```

#### `StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Runnable runnable)`

Start event listener with initial log message.

**Parameters:**
- `publisherBlock` - Event publisher block
- `eventListenerName` - Logical name for listener
- `message` - Optional log message for listener start
- `runnable` - Code to execute

**Thread Safety:** All methods are thread-safe

**Error Handling:** Exceptions are caught, logged, and re-thrown as RuntimeException

---

## Log

Static logging API for VFL with support for different log levels and functional logging.

### Basic Logging Methods

#### `Info(String message, Object... args)`

Log informational message.

**Parameters:**
- `message` - Message template with `{}` placeholders
- `args` - Arguments for placeholder replacement

**Example:**
```java
Log.Info("Processing user {} with status {}", userId, status);
```

#### `Warn(String message, Object... args)`

Log warning message.

**Example:**
```java
Log.Warn("Memory usage at {}% for user {}", memoryPercent, userId);
```

#### `Error(String message, Object... args)`

Log error message.

**Example:**
```java
Log.Error("Database connection failed for operation {}", operationId);
```

### Functional Logging Methods

#### `InfoFn(Supplier<R> fn, String message, Object... args)`

Execute supplier and log result using message template.

**Parameters:**
- `fn` - Supplier to execute
- `message` - Template with `{r}` for return value
- `args` - Additional arguments

**Returns:** `R` - Result from supplier

**Example:**
```java
String result = Log.InfoFn(() -> fetchData(), "Fetched data: {r}");
```

#### `InfoFn(Runnable runnable, String message, Object... args)`

Execute runnable and log completion message.

**Example:**
```java
Log.InfoFn(() -> saveData(), "Data saved successfully");
```

#### `WarnFn(Supplier<R> fn, Function<R, String> messageSerializer)`

Execute supplier and create warning message based on result.

**Parameters:**
- `fn` - Supplier to execute
- `messageSerializer` - Function to convert result to log message

**Example:**
```java
int count = Log.WarnFn(() -> getItemCount(), 
    count -> "Item count is low: " + count);
```

### Event Publishing

#### `Publish(String publisherName, String message, Object... args)`

Create and log an event publisher block for linking async processing.

**Parameters:**
- `publisherName` - Name of the publisher/event source
- `message` - Message template
- `args` - Arguments for formatting

**Returns:** `EventPublisherBlock` - Block representing the publish action

**Example:**
```java
EventPublisherBlock event = Log.Publish("Order Created", 
    "Order {} created with total ${}", orderId, total);
```

### Continuation Blocks

#### `CreateContinuationBlock(String blockName, Function<Block, R> fn)`

Create detached continuation block for cross-service tracing.

**Parameters:**
- `blockName` - Logical name for the continuation block
- `fn` - Function that receives the block and returns a result

**Returns:** `R` - Result from the function

**Example:**
```java
String response = Log.CreateContinuationBlock("Call External API", block -> {
    return httpClient.call("/api/data", serialize(block));
});
```

#### `CreateContinuationBlock(String blockName, String startMessage, Consumer<Block> consumer)`

Create continuation block with start message (void version).

**Parameters:**
- `blockName` - Logical name for the block
- `startMessage` - Message logged when block starts
- `consumer` - Consumer that receives the block

**Example:**
```java
Log.CreateContinuationBlock("Send Message", "Sending to queue", block -> {
    messageQueue.send(createMessage(block));
});
```

**Thread Safety:** All methods are thread-safe when called within VFL context

**Error Handling:** Returns without logging if VFL is not initialized

---

## VFLFutures

VFL-enabled CompletableFuture helpers that preserve trace context in asynchronous operations.

### Supply Methods

#### `supplyAsync(Supplier<R> supplier, Executor executor)`

Run supplier asynchronously with custom executor, creating JOIN sub-block.

**Parameters:**
- `supplier` - Code to execute asynchronously
- `executor` - Custom executor for task execution

**Returns:** `CompletableFuture<R>` - Future with VFL context preserved

**Example:**
```java
CompletableFuture<String> future = VFLFutures.supplyAsync(() -> {
    return performCalculation();
}, customExecutor);

String result = future.join();
```

#### `supplyAsync(Supplier<R> supplier)`

Run supplier asynchronously using common ForkJoin pool.

**Example:**
```java
CompletableFuture<Data> dataFuture = VFLFutures.supplyAsync(() -> {
    return fetchFromDatabase();
});
```

### Run Methods

#### `runAsync(Runnable runnable, Executor executor)`

Run task asynchronously creating NO_JOIN sub-block.

**Parameters:**
- `runnable` - Code to execute asynchronously
- `executor` - Custom executor for task execution

**Returns:** `CompletableFuture<Void>`

**Example:**
```java
CompletableFuture<Void> task = VFLFutures.runAsync(() -> {
    sendNotification();
}, notificationExecutor);
```

#### `runAsync(Runnable runnable)`

Run task asynchronously using common pool.

**Thread Safety:** Thread-safe, but must be called within active VFL block context

**Error Handling:** Logs warning if no parent context exists; task still executes without VFL tracing

---

## @SubBlock Annotation

Marks methods for automatic VFL sub-block creation with bytecode instrumentation.

### Attributes

#### `blockName` (optional)
- **Type:** `String`
- **Default:** Method name with arguments
- **Supports:** Placeholder patterns `{0}`, `{1}`, etc.

#### `startMessage` (optional)
- **Type:** `String`
- **Default:** No start message
- **Supports:** Argument placeholders

#### `endMessage` (optional)
- **Type:** `String`
- **Default:** No end message
- **Supports:** Argument and return value placeholders

### Examples

#### Basic Usage
```java
@SubBlock(blockName = "Process Payment {0}")
public void processPayment(String orderId) {
    // Method implementation
}
```

#### With Messages
```java
@SubBlock(
    blockName = "Calculate Tax for {0}",
    startMessage = "Starting tax calculation for order {0}",
    endMessage = "Tax calculated: ${r} for order {0}"
)
public BigDecimal calculateTax(String orderId) {
    return taxService.calculate(orderId);
}
```

**Thread Safety:** Methods are thread-safe; instrumentation handles concurrent execution

**Error Handling:** Logs warning if called outside VFL root block context

---

## Buffer Configuration

### VFLBuffer Interface

Base interface for all buffer implementations.

#### Methods
- `pushLogToBuffer(Log log)` - Buffer a log entry
- `pushBlockToBuffer(Block block)` - Buffer block creation
- `pushLogStartToBuffer(String blockId, long timestamp)` - Buffer block start
- `pushLogEndToBuffer(String blockId, BlockEndData endData)` - Buffer block end
- `flush()` - Flush all pending data

### SynchronousBuffer

Flushes data immediately in calling thread.

```java
VFLBuffer buffer = new SynchronousBuffer(100, flushHandler);
```

**Parameters:**
- `bufferSize` - Max items before auto-flush
- `flushHandler` - Handler for processing flushed data

**Thread Safety:** Thread-safe

### AsyncBuffer

Flushes data asynchronously with periodic flushing.

```java
VFLBuffer buffer = new AsyncBuffer(
    100,           // buffer size
    3000,          // flush timeout ms
    1000,          // periodic flush interval ms
    flushHandler,  // flush handler
    executor,      // flush executor
    scheduler      // periodic scheduler
);
```

**Thread Safety:** Thread-safe with concurrent flush operations

### NoOpsBuffer

Discards all data (testing/disabled mode).

```java
VFLBuffer buffer = new NoOpsBuffer();
```

---

## Flush Handlers

### VFLHubFlushHandler

Sends data to VFL Hub server via HTTP.

```java
VFLFlushHandler handler = new VFLHubFlushHandler(
    URI.create("http://localhost:8080")
);
```

**Thread Safety:** Thread-safe for concurrent requests

**Error Handling:** Returns false on network failures; logs detailed error information

### NestedJsonFlushHandler

Writes hierarchical JSON to file (development only).

```java
VFLFlushHandler handler = new NestedJsonFlushHandler("output.json");
```

**Thread Safety:** Thread-safe

**Error Handling:** Throws RuntimeException on file I/O errors

---

## Placeholder Patterns

VFL supports dynamic placeholder replacement in block names and messages.

### Argument Placeholders

- `{0}` - First method argument (0-indexed)
- `{1}` - Second method argument
- `{2}` - Third method argument
- etc.

**Example:**
```java
@SubBlock(blockName = "Process order {0} for user {1}")
public void processOrder(String orderId, String userId) {
    // Results in: "Process order ORD-123 for user USER-456"
}
```

### Return Value Placeholders

- `{r}` - Return value (endMessage only)
- `{return}` - Return value (case-insensitive)

**Example:**
```java
@SubBlock(endMessage = "Calculation result: {r}")
public BigDecimal calculate() {
    return new BigDecimal("123.45");
    // Results in: "Calculation result: 123.45"
}
```

### Null Handling

- `null` arguments become `"null"` in output
- `null` return values become `"null"` in output
- Invalid indices (e.g., `{5}` when only 2 arguments) remain unchanged

### Log Message Placeholders

Log methods use SLF4J-style `{}` placeholders:

```java
Log.Info("Processing {} items for user {}", itemCount, userId);
```

**Thread Safety:** Placeholder replacement is thread-safe

---

## Error Handling

### Common Exceptions

#### RuntimeException
**Thrown by:** VFLStarter methods  
**Cause:** Wrapped exceptions from user code
**Example:**
```java
VFLStarter.StartRootBlock("Operation", () -> {
    throw new IllegalArgumentException("Invalid data");
    // Becomes: RuntimeException wrapping IllegalArgumentException
});
```

#### IllegalStateException
**Thrown by:** `Log.CreateContinuationBlock()`
**Cause:** No active VFL context
**Example:**
```java
// Outside VFL context
Log.CreateContinuationBlock("Invalid", block -> {
    // Throws IllegalStateException
});
```

### Error Recovery

#### VFL Disabled/Not Initialized
When VFL is disabled or not initialized:
- `VFLStarter` methods execute code without tracing
- `Log` methods return immediately without logging
- `VFLFutures` methods behave like standard CompletableFuture
- `@SubBlock` methods execute normally without instrumentation

#### No Parent Context
When `@SubBlock` methods are called without parent context:
- Warning is logged: `"Could not create block for @SubBlock-{name}: no parent block"`
- Method executes normally without VFL tracing
- No exception is thrown

#### Network/Flush Failures
- Flush handlers return `false` on failure
- Errors are logged but don't interrupt application flow
- Async buffers may retry or fall back to synchronous flush

### Best Practices

1. **Always use try-catch for VFLStarter operations:**
```java
try {
    VFLStarter.StartRootBlock("Operation", () -> riskyOperation());
} catch (RuntimeException e) {
    // Handle wrapped exceptions
}
```

2. **Check VFL status in critical paths:**
```java
if (!VFLInitializer.isDisabled()) {
    // VFL operations
}
```

**Thread Safety:** Error handling is thread-safe across all components

---

## Thread Safety

### Thread Safety Guarantees

#### VFLStarter
- **Thread-safe:** All methods can be called concurrently
- **Context:** Each thread maintains independent block stack
- **Isolation:** Thread-local storage prevents context bleeding

#### Log
- **Thread-safe:** All logging methods are thread-safe
- **Context:** Operates on calling thread's VFL context
- **Buffering:** Thread-safe buffer operations

#### VFLFutures
- **Thread-safe:** Safe for concurrent usage
- **Context Transfer:** Automatically transfers VFL context to async threads
- **Isolation:** Each async task gets independent context copy

#### @SubBlock Annotation
- **Thread-safe:** Instrumented methods handle concurrent execution
- **Context:** Uses calling thread's context
- **Stack Management:** Thread-local block stack management

#### Buffers
- **SynchronousBuffer:** Thread-safe with concurrent flush operations
- **AsyncBuffer:** Thread-safe with async flush execution
- **NoOpsBuffer:** Thread-safe (no-op implementation)

#### Flush Handlers
- **VFLHubFlushHandler:** Thread-safe HTTP operations
- **NestedJsonFlushHandler:** Thread-safe file operations

### Context Propagation

VFL maintains execution context using `ThreadLocal` storage:

```java
// Main thread context
VFLStarter.StartRootBlock("Main", () -> {
    
    // Context propagates to @SubBlock methods
    service.processData();
    
    // Context must be explicitly transferred to async operations
    VFLFutures.supplyAsync(() -> {
        // New thread gets copy of main thread's context
        return service.asyncOperation();
    });
    
    // Standard CompletableFuture loses context
    CompletableFuture.supplyAsync(() -> {
        // No VFL context here - logs will be ignored
        return service.asyncOperation();
    });
});
```

### Concurrent Access Patterns

#### Safe Patterns
```java
// Multiple threads with separate contexts
executor.submit(() -> {
    VFLStarter.StartRootBlock("Task 1", () -> work1());
});

executor.submit(() -> {
    VFLStarter.StartRootBlock("Task 2", () -> work2());
});

// Shared service methods with @SubBlock
@SubBlock
public synchronized void sharedMethod() {
    // Safe: synchronization handled by application
}
```

#### Unsafe Patterns
```java
// DON'T: Share Block objects between threads
Block block = getCurrentBlock();
executor.submit(() -> {
    VFLStarter.ContinueFromBlock(block, () -> work()); // Potentially unsafe
});

// DON'T: Manual context manipulation
ThreadContextManager.PushBlockToThreadLogStack(block); // Internal API
```

### Memory Model

- **Thread-Local Storage:** Each thread has independent VFL context
- **No Shared State:** Block contexts are not shared between threads
- **Atomic Operations:** Buffer operations use appropriate synchronization
- **Context Cleanup:** Automatic cleanup when threads complete VFL operations

**Best Practice:** Always use VFL-provided methods for context management and async operations.