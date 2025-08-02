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

**Block** (`dev.kuku.vfl.core.models.Block`)
- Simple data class representing execution scopes
- **Fields**:
  - `String id`: Unique block identifier
  - `String parentBlockId`: Reference to parent block (null for root blocks)
  - `String blockName`: Human-readable block name
- **Purpose**: Creates hierarchical structure for execution flow tracking

**Log** (`dev.kuku.vfl.core.models.logs.Log`)
- Base class for all log entries within blocks
- **Fields**:
  - `String id`: Unique log identifier
  - `String blockId`: Reference to containing block
  - `String parentLogId`: Reference to parent log (for nested structure)
  - `LogType logType`: Type wrapper containing enum value
  - `String message`: Log content
  - `long timestamp`: Epoch milliseconds timestamp
- **Constructors**: Support both LogType object and LogTypeEnum enum
- **Purpose**: Individual log entries that build the execution narrative

**SubBlockStartLog** (`dev.kuku.vfl.core.models.logs.SubBlockStartLog`)
- Specialized log for marking sub-block initiation
- **Extends**: Log
- **Additional Field**:
  - `String referencedBlockId`: ID of the block being started
- **Purpose**: Links parent blocks to their sub-blocks for flow visualization
- **Constructors**:
  - Direct creation with LogTypeBlockStartEnum
  - Conversion from existing Log with block start type

**LogType** (`dev.kuku.vfl.core.models.logs.LogType`)
- Wrapper class for log type enums with JSON serialization support
- **Field**: `String value` (final)
- **Constructors**: Accepts LogTypeEnum or LogTypeBlockStartEnum
- **JSON Support**: `@JsonValue` and `@JsonCreator` annotations for proper serialization
- **Purpose**: Unified type system for different log categories

**VFLBlockContext** (`dev.kuku.vfl.core.dtos.VFLBlockContext`)
- Runtime context for active blocks
- **Fields**:
  - `Block blockInfo`: Associated block metadata
  - `AtomicBoolean blockStarted`: Thread-safe block initialization flag
  - `String currentLogId`: Current log identifier for nested logging
  - `VFLBuffer buffer`: Buffer instance for log output
- **Purpose**: Maintains runtime state and buffer reference for active blocks

**BlockEndData** (`dev.kuku.vfl.core.dtos.BlockEndData`)
- Data structure for block completion information
- **Fields**:
  - `Long endTime`: Block completion timestamp
  - `String endMessage`: Optional completion message
- **Purpose**: Encapsulates block termination data for consistent handling

**EventPublisherBlock** (`dev.kuku.vfl.core.dtos.EventPublisherBlock`)
- Record wrapper for event-driven logging
- **Field**: `Block block`: Wrapped block instance
- **Purpose**: Marker type for event publisher pattern integration

## Refined Buffer System Architecture

### 1. VFLBuffer Interface (`dev.kuku.vfl.core.buffer.VFLBuffer`)
**Core Operations**:
- `pushLogToBuffer(Log log)`: Add log entries (fire-and-forget)
- `pushBlockToBuffer(Block block)`: Add block metadata
- `pushLogStartToBuffer(String blockId, long timestamp)`: Record block start times
- `pushLogEndToBuffer(String blockId, BlockEndData endData)`: Record block completion
- `flushAndClose()`: Final flush and cleanup

### 2. Abstract Base Classes

#### VFLBufferBase (`dev.kuku.vfl.core.buffer.abstracts.VFLBufferBase`)
**Purpose**: Foundation for all buffer implementations
**Key Features**:
- Thread-safe data collection with ReentrantLock
- Automatic flushing when buffer size exceeded
- Separate collections for logs, blocks, block starts, and block ends
- Template method pattern with `onFlushAll()` hook

#### VFLBufferWithFlushHandlerBase (`dev.kuku.vfl.core.buffer.abstracts.VFLBufferWithFlushHandlerBase`)
**Purpose**: Adds flush handler integration and enforced ordering
**Key Features**:
- Enforced flush order: blocks → block starts → block ends → logs
- `performOrderedFlush()` helper method for consistent ordering
- Abstract `executeFlushAll()` for execution strategy customization
- Automatic flush handler cleanup on close

### 3. Concrete Buffer Implementations

#### SynchronousVFLBuffer (`dev.kuku.vfl.core.buffer.SynchronousVFLBuffer`)
**Purpose**: Simple synchronous flushing
**Execution**: Direct flush calls in current thread
**Use Case**: Development, testing, or single-threaded applications

#### AsyncVFLBuffer (`dev.kuku.vfl.core.buffer.AsyncVFLBuffer`)
**Purpose**: Asynchronous flushing with periodic scheduling
**Key Features**:
- **Periodic Flushing**: Scheduled flush intervals
- **Graceful Shutdown**: Timeout-based executor termination
- **Fallback Strategy**: Synchronous flush if executor unavailable
- **Proper Cleanup**: Sequential executor shutdown (periodic → flush → handler)

**Configuration Parameters**:
- `bufferSize`: Threshold for automatic flushing
- `finalFlushTimeoutMillisecond`: Maximum wait time for shutdown
- `periodicFlushTimeMillisecond`: Interval for scheduled flushes
- `bufferFlushExecutor`: Executor for flush operations
- `periodicFlushExecutor`: Scheduler for periodic operations

## Enhanced Flush Handler System

### VFLFlushHandler Interface (`dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler`)
**Operations**:
- `pushLogsToServer(List<Log> logs)`: Handle mixed log types (Log + SubBlockStartLog)
- `pushBlocksToServer(List<Block> blocks)`: Handle block metadata
- `pushBlockStartsToServer(Map<String, Long> blockStarts)`: Handle timing data
- `pushBlockEndsToServer(Map<String, BlockEndData> blockEnds)`: Handle completion data
- `closeFlushHandler()`: Cleanup resources

### Concrete Implementations

#### NestedJsonFlushHandler (`dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler`)
**Purpose**: Development/testing output as hierarchical JSON
**Key Features**:
- **Hierarchical Structure**: Builds nested JSON mirroring execution flow
- **Time Formatting**: Human-readable timestamps and durations
- **Referenced Blocks**: Full block details for SubBlockStartLog entries
- **File Output**: Creates parent directories automatically

**JSON Structure**:
```json
[
  {
    "blockId": "b1",
    "parentBlockId": null,
    "name": "root_block",
    "startTime": "2024-01-01 10:00:00.123",
    "endTime": "2024-01-01 10:00:05.456",
    "endMessage": "Operation completed",
    "logsChain": [
      {
        "id": "log1",
        "type": "MESSAGE",
        "message": "Starting operation",
        "logsChain": [...] // Nested logs
      },
      {
        "id": "log2",
        "type": "SUB_BLOCK_START_PRIMARY",
        "message": "Starting sub-operation",
        "duration": "2.5s",
        "endMessage": "Sub-operation completed",
        "referencedBlock": { /* Full block details */ }
      }
    ]
  }
]
```

#### VFLHubFlushHandler (`dev.kuku.vfl.core.buffer.flushHandler.VFLHubFlushHandler`)
**Purpose**: Production integration with VFL Server
**Key Features**:
- **HTTP Client**: Blocking HttpClient for ordered requests
- **RESTful API**: Separate endpoints for different data types
- **Error Handling**: Comprehensive logging and exception handling
- **Status Validation**: HTTP status code verification

**API Endpoints**:
- `POST /api/v1/logs`: Log entries
- `POST /api/v1/blocks`: Block metadata
- `POST /api/v1/block-starts`: Block start times
- `POST /api/v1/block-ends`: Block completion data

## Log Type System

### 1. LogTypeEnum (`dev.kuku.vfl.core.models.logs.enums.LogTypeEnum`)
**Purpose**: Standard log message types
**Values**:
- `MESSAGE("MESSAGE")`: Regular informational logs
- `WARN("WARN")`: Warning level logs
- `ERROR("ERROR")`: Error level logs

### 2. LogTypeBlockStartEnum (`dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum`)
**Purpose**: Sub-block start operation types
**Values**:
- `SUB_BLOCK_START_PRIMARY("SUB_BLOCK_START_PRIMARY")`: Sequential sub-block execution
- `SUB_BLOCK_START_SECONDARY_JOIN("SUB_BLOCK_START_SECONDARY_JOIN")`: Parallel execution with join
- `SUB_BLOCK_START_SECONDARY_NO_JOIN("SUB_BLOCK_START_SECONDARY_NO_JOIN")`: Fire-and-forget execution
- `PUBLISH_EVENT("PUBLISH_EVENT")`: Event publisher initiation
- `EVENT_LISTENER("EVENT_LISTENER")`: Event listener registration

### 3. LogType Wrapper Pattern
The LogType class provides a unified interface with JSON serialization:
```java
// Standard logging
new LogType(LogTypeEnum.MESSAGE)

// Block start logging  
new LogType(LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY)

// JSON serialization support
@JsonValue toString() // Serializes as string value
@JsonCreator fromString(String) // Deserializes from string
```

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

**Enhanced Helper Class**: `VFLHelper` provides static utilities for:
- Log creation and buffer operations
- Block creation and management
- Function execution with automatic cleanup (`CallFnWithLogger`)

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
- Support for secondary blocks (joining and non-joining)
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
- **Enhanced Debugging**: Detailed logging of stack operations with thread info

**ThreadLocal Management**:
```java
private static final ThreadLocal<Stack<ThreadVFL>> loggerStack = new ThreadLocal<>();

// Enhanced logging with thread info and trimmed IDs
log.debug("PUSH: Added logger '{}' to existing stack {} - Stack size: {}", 
    trimId(subLoggerCtx.blockInfo.getId()), getThreadInfo(), stack.size());
```

#### 2. PassVFL (Extends VFLFn)
**Location**: `dev.kuku.vfl.variants.PassVFL`
**Purpose**: Manual logger passing approach
**Key Features**:
- **Explicit Control**: Manual logger lifecycle management
- **Legacy Support**: Works with systems that can't use ThreadLocal
- **Function Pattern**: Logger passed as function parameter
- **Singleton Runner**: Static runner instance for convenience

## Enhanced Fluent API System

### 1. FluentVFL (Base Fluent Class)
**Location**: `dev.kuku.vfl.core.fluent_api.base.FluentVFL`
**Purpose**: Universal fluent wrapper for any VFL instance
**Key Features**:
- Parameter substitution in log messages with `FormatMessage`
- Wraps any VFL implementation
- Basic fluent operations for call() and run()

### 2. FluentVFLCallable (Extends FluentVFL)
**Location**: `dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable`
**Purpose**: Fluent API for VFLCallable instances
**Key Features**:
- Enhanced supplier and runnable steps
- Sub-block fluent chains
- Method chaining for configuration

### 3. FluentThreadVFL (Static Wrapper)
**Location**: `dev.kuku.vfl.variants.thread_local.FluentThreadVFL`
**Purpose**: Static fluent API for ThreadVFL
**Key Features**:
- **Static Methods**: No instantiation needed
- **Automatic Context**: Uses ThreadVFL's ThreadLocal context
- **Fluent Chaining**: `Call()`, `Run()` with method chains

**Usage Pattern**:
```java
// Static fluent API - no manual instantiation
String result = FluentThreadVFL.Call(() -> processData())
    .asSubBlock("Data Processing")
    .withStartMessage("Starting processing")
    .withEndMessageMapper(r -> "Completed: " + r)
    .startPrimary();
```

### 4. Fluent API Step Classes

#### AsSubBlockStep
**Location**: `dev.kuku.vfl.core.fluent_api.callable.steps.AsSubBlockStep`
**Purpose**: Configurable sub-block execution
**Key Features**:
- Start message configuration
- End message mapping with parameters
- Multiple execution strategies (primary, secondary joining, secondary non-joining)

#### CallableSupplierStep and SupplierStep
**Purpose**: Function execution with logging
**Features**:
- `asLog()`, `asError()`, `asWarning()` methods
- Message serialization support
- Parameter passing for formatted messages

## Enhanced Runner System

### Abstract Base Classes

#### VFLRunner (`dev.kuku.vfl.core.vfl_abstracts.runner.VFLRunner`)
**Purpose**: Base class for runner implementations
**Key Feature**: `initRootCtx()` for consistent root context creation

#### VFLCallableRunner (`dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner`)
**Purpose**: Runner for Callable-style VFL implementations
**Methods**:
- `startVFL()`: For supplier and runnable execution
- `startEventListenerLogger()`: For event-driven logging
- Abstract methods for logger creation

#### VFLFnRunner (`dev.kuku.vfl.core.vfl_abstracts.runner.VFLFnRunner`)
**Purpose**: Runner for Function-style VFL implementations
**Methods**:
- `startVFL()`: With function and consumer variants
- `startEventListenerLogger()`: For event-driven logging
- Abstract methods for logger creation

### Concrete Runner Implementations

Both ThreadVFL.Runner and PassVFL.Runner extend their respective abstract runners and provide:
- Static convenience methods
- Automatic buffer cleanup with `finally` blocks
- Event listener support
- Proper context management

## Block Types & Flow Control

### Execution Pattern Mapping

#### 1. Primary Sub-blocks (Sequential)
- **Log Type**: `SUB_BLOCK_START_PRIMARY`
- **Behavior**: Blocks parent execution, same thread
- **Context**: Pushed to existing logger stack
- **Use Case**: Main execution flow operations

#### 2. Secondary Joining Blocks (Parallel)
- **Log Type**: `SUB_BLOCK_START_SECONDARY_JOIN`
- **Behavior**: Returns CompletableFuture, new thread
- **Context**: New logger stack in executor thread
- **Use Case**: Parallel operations requiring results

#### 3. Secondary Non-Joining Blocks (Fire-and-Forget)
- **Log Type**: `SUB_BLOCK_START_SECONDARY_NO_JOIN`
- **Behavior**: Returns CompletableFuture<Void>, new thread
- **Context**: New logger stack, no result expected
- **Use Case**: Background operations (logging, cleanup)

#### 4. Event Publisher/Listener Pattern
- **Publisher**: `PUBLISH_EVENT`, creates EventPublisherBlock
- **Listener**: `EVENT_LISTENER`, handles published events
- **Threading**: Depends on event system implementation
- **Flow**: Decoupled execution with event data passing

## Key Design Patterns

### 1. Template Method Pattern
- **VFLBufferBase**: `onFlushAll()` hook for subclass customization
- **VFLBufferWithFlushHandlerBase**: `executeFlushAll()` for execution strategy
- **VFL.logInternal()**: Common logging logic with subclass hooks

### 2. Strategy Pattern
- **VFLFlushHandler**: Different output strategies (JSON, server, memory)
- **VFLBuffer**: Different buffering strategies (sync, async)
- **Runner**: Different logger management strategies

### 3. Builder/Fluent Pattern
- **FluentVFL**: Method chaining for readable configuration
- **AsSubBlockStep**: Fluent configuration of sub-blocks
- **Step Classes**: Progressive configuration building

### 4. Factory Pattern
- **VFLHelper**: Static factory methods for creating logs/blocks
- **Runners**: Factory methods for creating root/event loggers

### 5. ThreadLocal Pattern (ThreadVFL)
- Automatic context management without manual passing
- Stack-based nested context handling
- Thread-safe parallel execution with proper cleanup

## Thread Safety & Concurrency

### Buffer Thread Safety
- **ReentrantLock**: Protects shared data structures
- **Atomic Operations**: Where appropriate for performance
- **Lock Minimization**: Quick copy-and-clear strategy

### ThreadVFL Thread Management
```java
// Enhanced debugging with thread information
private static String getThreadInfo() {
    Thread currentThread = Thread.currentThread();
    return String.format("[Thread: %s (ID: %d)]", 
        currentThread.getName(), currentThread.threadId());
}

// Stack operations with detailed logging
log.debug("PUSH: Added logger '{}' to existing stack {} - Stack size: {}", 
    trimId(subLoggerCtx.blockInfo.getId()), getThreadInfo(), stack.size());
```

### Async Buffer Concurrency
- **Executor Management**: Proper shutdown sequences
- **Fallback Strategy**: Synchronous execution when executors unavailable
- **Timeout Handling**: Graceful shutdown with configurable timeouts

## Configuration & Setup

### Recommended Setup Pattern
```java
// 1. Create flush handler
VFLFlushHandler flushHandler = new NestedJsonFlushHandler("logs/output.json");
// or for production:
// VFLFlushHandler flushHandler = new VFLHubFlushHandler(serverUri);

// 2. Create buffer
VFLBuffer buffer = new AsyncVFLBuffer(
    100,           // bufferSize
    5000,          // finalFlushTimeoutMs
    1000,          // periodicFlushMs
    flushHandler,
    flushExecutor,
    periodicExecutor
);

// 3. Use with ThreadVFL (recommended)
ThreadVFL.Runner.StartVFL("Operation", buffer, () -> {
    // Your application logic with automatic context management
    ThreadVFL.Log("Starting operation");
    return ThreadVFL.CallPrimarySubBlock("SubOperation", "msg", 
        () -> { /* sub-operation */ return result; }, 
        r -> "Completed: " + r);
});
```

## Error Handling & Cleanup

### Automatic Cleanup
- **Buffer System**: Automatic flushing on size/time limits
- **ThreadLocal**: Automatic cleanup on block completion
- **Executors**: Proper shutdown with timeout handling
- **Runners**: Guaranteed `buffer.flushAndClose()` in finally blocks

### Exception Handling
- **VFLHelper.CallFnWithLogger()**: Catches exceptions, logs them, ensures cleanup
- **Buffer Operations**: Fire-and-forget with fallback strategies
- **HTTP Client**: Comprehensive error logging and status validation

### Graceful Degradation
- **Executor Shutdown**: Synchronous fallback when async unavailable
- **Network Failures**: Logged but don't break application flow
- **Serialization Errors**: Logged with fallback messages

## Usage Recommendations

### Use ThreadVFL + FluentThreadVFL when:
- Building new applications requiring automatic context management
- Need clean, readable code with static API
- Working with multi-threaded applications
- Want comprehensive debugging and monitoring

### Use PassVFL when:
- Working with legacy systems requiring explicit control
- Building frameworks or libraries
- Cannot use ThreadLocal (rare edge cases)
- Need maximum flexibility in logger lifecycle management

### Use AsyncVFLBuffer when:
- Production environments with high throughput
- Network-based flush handlers (VFLHubFlushHandler)
- Performance is critical
- Can tolerate eventual consistency

### Use SynchronousVFLBuffer when:
- Development and testing environments
- Immediate consistency required
- Simple single-threaded applications
- Local file output (NestedJsonFlushHandler)

This updated context reflects the significant architectural improvements, enhanced error handling, better thread safety, and more flexible configuration options in the VFL codebase.