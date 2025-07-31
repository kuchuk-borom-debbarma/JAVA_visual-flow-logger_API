# VFL (Visual Flow Logger) Codebase Context

## Project Overview
Visual Flow Logger (VFL) is a **hierarchical logging framework** that creates structured logs mirroring application execution flow. Unlike traditional flat logging, VFL organizes logs into meaningful blocks showing method execution flow, parallel operations, timing relationships, and context preservation.

## Core Architecture

### 1. Layered Architecture
```
┌─────────────────────────────────────────┐
│           User API Layer                │
│  (ThreadVFL, PassVFL, FluentThreadVFL)  │
├─────────────────────────────────────────┤
│         Abstract VFL Layer              │
│    (VFL, VFLCallable, VFLFn)           │
├─────────────────────────────────────────┤
│          Buffer Layer                   │
│  (VFLBuffer implementations)            │
├─────────────────────────────────────────┤
│        Flush Handler Layer              │
│    (VFLFlushHandler implementations)    │
└─────────────────────────────────────────┘
```

### 2. Key Components

#### Core Data Models
- **Block**: Represents execution scopes with ID, name, parent relationships
- **Log**: Individual log entries within blocks with timestamps and types
- **VFLBlockContext**: Contains block info and buffer reference
- **EventPublisherBlock**: Special block type for event-driven patterns

#### Buffer System
- **VFLBuffer Interface**: Defines buffer operations
- **ThreadSafeAsyncVFLBuffer**: Asynchronous, thread-safe buffer with periodic flushing
- **ThreadSafeSynchronousVflBuffer**: Simple synchronous buffer

#### Flush Handler System
- **VFLFlushHandler Interface**: Defines output destinations
- **InMemoryFlushHandler**: For development/testing
- **VFLHubFlushHandler**: For production (sends to VFL Server)

## Class Hierarchy & Relationships

### Abstract Base Classes

#### 1. VFL (Base Abstract Class)
**Location**: `dev.kuku.vfl.core.vfl_abstracts.VFL`
**Purpose**: Foundation class providing basic logging functionality
**Key Features**:
- Thread-safe block lifecycle management (`ensureBlockStarted()`)
- Basic logging methods: `log()`, `warn()`, `error()`
- Function logging: `logFn()`, `warnFn()`, `errorFn()`
- Abstract `getContext()` method for subclass implementation

**Core Pattern**:
```java
// Ensures block is started before any logging
public final void ensureBlockStarted() {
    if (blockStarted.compareAndSet(false, true)) {
        getContext().buffer.pushLogStartToBuffer(...);
    }
}
```

#### 2. VFLCallable (Extends VFL)
**Location**: `dev.kuku.vfl.core.vfl_abstracts.VFLCallable`
**Purpose**: Adds sub-block creation and parallel execution capabilities
**Key Features**:
- Primary sub-blocks (sequential): `callPrimarySubBlock()`
- Secondary joining blocks (parallel, returns CompletableFuture): `callSecondaryJoiningBlock()`
- Secondary non-joining blocks (fire-and-forget): `callSecondaryNonJoiningBlock()`
- Event publisher blocks: `createEventPublisherBlock()`

**Abstract Methods**:
- `getLogger()`: Returns logger instance for sub-blocks
- `prepareLoggerAfterSubBlockStartDataInitializedAndPushed()`: Handles logger context setup

#### 3. VFLFn (Extends VFL)
**Location**: `dev.kuku.vfl.core.vfl_abstracts.VFLFn`
**Purpose**: Function-based approach where logger is passed as parameter
**Key Features**:
- Function-style sub-blocks: `callPrimarySubBlock(Function<VFLFn, R> fn)`
- Manual logger passing pattern
- Abstract `getLogger()` for subclass implementation

### Concrete Implementations

#### 1. ThreadVFL (Extends VFLCallable)
**Location**: `dev.kuku.vfl.variants.thread_local.ThreadVFL`
**Purpose**: ThreadLocal-based automatic context management
**Key Features**:
- **ThreadLocal Stack**: Uses `ThreadLocal<Stack<ThreadVFL>>` for context
- **Static Methods**: All operations available as static methods
- **Automatic Context**: No need to pass loggers manually
- **Thread Management**: Handles thread creation/cleanup for parallel operations

**ThreadLocal Management**:
```java
private static final ThreadLocal<Stack<ThreadVFL>> loggerStack = new ThreadLocal<>();

// Push new logger for sub-blocks
ThreadVFL.loggerStack.get().push(newLogger);

// Pop when block completes
ThreadVFL.loggerStack.get().pop();
```

**Static API Pattern**:
```java
// Static methods delegate to current thread's logger
public static void Log(String message) {
    getCurrentLogger().log(message);
}
```

#### 2. PassVFL (Extends VFLFn)
**Location**: `dev.kuku.vfl.variants.PassVFL`
**Purpose**: Manual logger passing approach
**Key Features**:
- **Explicit Control**: Manual logger lifecycle management
- **Legacy Support**: Works with systems that can't use ThreadLocal
- **Function Pattern**: Logger passed as function parameter

### Fluent API System

#### 1. FluentVFL (Base Fluent Class)
**Location**: `dev.kuku.vfl.core.fluent_api.base.FluentVFL`
**Purpose**: Universal fluent wrapper for any VFL instance
**Key Features**:
- Parameter substitution in log messages
- Wraps any VFL implementation
- Manual instantiation required

#### 2. FluentVFLCallable (Extends FluentVFL)
**Location**: `dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable`
**Purpose**: Fluent API for VFLCallable instances
**Key Features**:
- Sub-block fluent chains
- Method chaining for configuration

#### 3. FluentThreadVFL (Static Wrapper)
**Location**: `dev.kuku.vfl.variants.thread_local.FluentThreadVFL`
**Purpose**: Static fluent API for ThreadVFL
**Key Features**:
- **Static Methods**: No instantiation needed
- **Automatic Context**: Uses ThreadVFL's ThreadLocal context
- **Fluent Chaining**: `Call()`, `RunSubBlock()` with method chains

**Usage Pattern**:
```java
// Static fluent API - no manual instantiation
String result = FluentThreadVFL.Call(() -> processData())
    .asSubBlock("Data Processing")
    .withStartMessage("Starting processing")
    .withEndMessageMapper(r -> "Completed: " + r)
    .startPrimary();
```

### Runner System

#### VFLRunner Hierarchy
- **VFLRunner**: Base abstract class with `initRootCtx()`
- **VFLCallableRunner**: For Callable-style VFL implementations
- **VFLFnRunner**: For Function-style VFL implementations

**Runner Pattern**:
```java
ThreadVFL.Runner.Instance.StartVFL("Operation", buffer, () -> {
    // Your logging code here
    return result;
});
```

## Block Types & Flow Control

### 1. Primary Sub-blocks (Sequential)
- **Purpose**: Main execution flow operations
- **Behavior**: Blocks parent execution until complete
- **Thread**: Same thread as parent
- **Logger Management**: Pushed to existing stack

### 2. Secondary Joining Blocks (Parallel)
- **Purpose**: Parallel operations that need to join back
- **Behavior**: Returns CompletableFuture, parent can wait
- **Thread**: New thread (uses executor)
- **Logger Management**: New stack in new thread

### 3. Secondary Non-Joining Blocks (Fire-and-Forget)
- **Purpose**: Background operations (logging, cleanup)
- **Behavior**: Returns CompletableFuture<Void>, no joining needed
- **Thread**: New thread (uses executor)
- **Logger Management**: New stack in new thread

### 4. Event Publisher/Listener Pattern
- **Purpose**: Event-driven architectures
- **Publisher**: Creates EventPublisherBlock
- **Listeners**: Multiple listeners can handle same event
- **Thread**: Depends on implementation (same or different thread)

## Key Design Patterns

### 1. Strategy Pattern
- **VFLFlushHandler**: Different output strategies (memory, file, server)
- **VFLBuffer**: Different buffering strategies (sync, async)

### 2. Template Method Pattern
- **VFL.logInternal()**: Common logging logic with subclass hooks
- **VFLHelper.CallFnWithLogger()**: Common function execution with cleanup

### 3. ThreadLocal Pattern (ThreadVFL)
- Automatic context management without manual passing
- Stack-based nested context handling
- Thread-safe parallel execution

### 4. Builder/Fluent Pattern
- **FluentVFL**: Method chaining for readable configuration
- **AsSubBlockStep**: Fluent configuration of sub-blocks

### 5. Factory Pattern
- **VFLHelper**: Static factory methods for creating logs/blocks
- **Runners**: Factory methods for creating root/event loggers

## Thread Safety & Concurrency

### ThreadVFL Thread Management
```java
// Main thread - uses existing stack
callPrimarySubBlock() -> stack.push(newLogger)

// New thread - creates new stack if needed
callSecondaryJoiningBlock() -> CompletableFuture with new stack

// Thread cleanup
close() -> stack.pop(), remove ThreadLocal if empty
```

### Buffer Thread Safety
- **ThreadSafeAsyncVFLBuffer**: Uses synchronization and async flushing
- **Lock Management**: Synchronized blocks for data consistency
- **Executor Management**: Proper shutdown with timeout

## Configuration & Setup

### Recommended Setup Pattern
```java
// 1. Create flush handler
ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();

// 2. Create buffer
VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(
    bufferSize, flushInterval, flushHandler, executor
);

// 3. Use with ThreadVFL (recommended)
ThreadVFL.Runner.Instance.StartVFL("Operation", buffer, () -> {
    // Your application logic
});
```

## Key Interfaces & Contracts

### VFLBuffer Contract
```java
void pushLogToBuffer(Log log);           // Add log entry
void pushBlockToBuffer(Block block);     // Add block entry  
void pushLogStartToBuffer(...);          // Mark block start
void pushLogEndToBuffer(...);            // Mark block end
void flushAndClose();                    // Cleanup
```

### VFLFlushHandler Contract
```java
boolean pushLogsToServer(List<Log> logs);
boolean pushBlocksToServer(List<Block> blocks);
boolean pushBlockStartsToServer(Map<String, Long> blockStarts);
boolean pushBlockEndsToServer(Map<String, String> blockEnds);
```

## Recommendations for Usage

### Use ThreadVFL + FluentThreadVFL when:
- Building new applications
- Want automatic context management
- Need clean, readable code
- Working with multi-threaded applications

### Use PassVFL when:
- Working with legacy systems
- Need explicit control over logger lifecycle
- Building frameworks or libraries
- Can't use ThreadLocal (rare cases)

### Use FluentVFL when:
- Want fluent API with PassVFL
- Need explicit logger management with fluent syntax
- Building flexible frameworks

## Error Handling & Cleanup

### Automatic Cleanup
- **ThreadVFL**: Automatic ThreadLocal cleanup on block completion
- **Buffer**: Automatic flushing on size limits or intervals
- **Runner**: Ensures `buffer.flushAndClose()` in finally blocks

### Exception Handling
- **VFLHelper.CallFnWithLogger()**: Catches exceptions, logs them, ensures cleanup
- **Buffer Operations**: Fire-and-forget pattern for buffer operations
- **Thread Management**: Proper executor shutdown with timeouts

This context provides a comprehensive understanding of the VFL codebase architecture, patterns, and usage recommendations for effective development and maintenance.