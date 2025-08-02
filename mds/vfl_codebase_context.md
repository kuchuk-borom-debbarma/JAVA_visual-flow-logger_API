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
- Wrapper class for log type enums
- **Field**: `String value` (final)
- **Constructors**: Accepts LogTypeEnum or LogTypeBlockStartEnum
- **Purpose**: Unified type system for different log categories

**VFLBlockContext** (`dev.kuku.vfl.core.dtos.VFLBlockContext`)
- Runtime context for active blocks
- **Fields**:
    - `Block blockInfo`: Associated block metadata
    - `AtomicBoolean blockStarted`: Thread-safe block initialization flag
    - `String currentLogId`: Current log identifier for nested logging
    - `VFLBuffer buffer`: Buffer instance for log output
- **Purpose**: Maintains runtime state and buffer reference for active blocks

**EventPublisherBlock** (`dev.kuku.vfl.core.dtos.EventPublisherBlock`)
- Record wrapper for event-driven logging
- **Field**: `Block block`: Wrapped block instance
- **Purpose**: Marker type for event publisher pattern integration

#### Buffer System
- **VFLBuffer Interface**: Defines buffer operations
- **ThreadSafeAsyncVFLBuffer**: Asynchronous, thread-safe buffer with periodic flushing
- **ThreadSafeSynchronousVflBuffer**: Simple synchronous buffer

#### Flush Handler System
- **VFLFlushHandler Interface**: Defines output destinations
- **InMemoryFlushHandler**: For development/testing
- **VFLHubFlushHandler**: For production (sends to VFL Server)

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
The LogType class provides a unified interface for both enum types:
```java
// Standard logging
new LogType(LogTypeEnum.MESSAGE)

// Block start logging  
new LogType(LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY)
```

## Data Model Relationships

### Hierarchical Structure
```
Block (Root)
├── Log entries (MESSAGE/WARN/ERROR)
├── SubBlockStartLog (references child Block)
│   └── Child Block
│       ├── Log entries
│       └── SubBlockStartLog (references grandchild)
└── More Log entries
```

### Block-to-Block Relationships
- **Parent-Child**: `Block.parentBlockId` references parent `Block.id`
- **Log-to-Block**: `Log.blockId` references containing `Block.id`
- **Log-to-Log**: `Log.parentLogId` references parent `Log.id` (for nested logs)
- **SubBlock Reference**: `SubBlockStartLog.referencedBlockId` references started `Block.id`

### Context Flow
```java
VFLBlockContext context = new VFLBlockContext(block, buffer);
context.blockStarted.compareAndSet(false, true); // Thread-safe initialization
context.currentLogId = "log-123"; // Track current log for nesting
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

### Log Type to Execution Pattern Mapping

#### 1. Primary Sub-blocks (Sequential)
- **Log Type**: `LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY`
- **Purpose**: Main execution flow operations
- **Behavior**: Blocks parent execution until complete
- **Thread**: Same thread as parent
- **Logger Management**: Pushed to existing stack
- **SubBlockStartLog**: References the started block via `referencedBlockId`

#### 2. Secondary Joining Blocks (Parallel)
- **Log Type**: `LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN`
- **Purpose**: Parallel operations that need to join back
- **Behavior**: Returns CompletableFuture, parent can wait
- **Thread**: New thread (uses executor)
- **Logger Management**: New stack in new thread
- **SubBlockStartLog**: Marks parallel execution start

#### 3. Secondary Non-Joining Blocks (Fire-and-Forget)
- **Log Type**: `LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN`
- **Purpose**: Background operations (logging, cleanup)
- **Behavior**: Returns CompletableFuture<Void>, no joining needed
- **Thread**: New thread (uses executor)
- **Logger Management**: New stack in new thread
- **SubBlockStartLog**: Marks fire-and-forget execution

#### 4. Event Publisher/Listener Pattern
- **Publisher Log Type**: `LogTypeBlockStartEnum.PUBLISH_EVENT`
- **Listener Log Type**: `LogTypeBlockStartEnum.EVENT_LISTENER`
- **Purpose**: Event-driven architectures
- **Publisher**: Creates EventPublisherBlock, logs PUBLISH_EVENT
- **Listeners**: Multiple listeners can handle same event, log EVENT_LISTENER
- **Thread**: Depends on implementation (same or different thread)
- **Flow**: EventPublisherBlock wraps Block for event-specific handling

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
void pushLogToBuffer(Log log);                    // Add standard log entry
void pushBlockToBuffer(Block block);              // Add block metadata
void pushLogStartToBuffer(...);                   // Mark block start (creates SubBlockStartLog internally)
void pushLogEndToBuffer(...);                     // Mark block end
void flushAndClose();                             // Cleanup and flush remaining data
```

**Log Creation Flow**:
1. Standard logs: Direct `Log` creation with `LogTypeEnum`
2. Sub-block starts: `SubBlockStartLog` creation with `LogTypeBlockStartEnum`
3. Buffer operations: Internal conversion to appropriate log types

### VFLFlushHandler Contract
```java
boolean pushLogsToServer(List<Log> logs);                    // Handle all log types (Log + SubBlockStartLog)
boolean pushBlocksToServer(List<Block> blocks);              // Handle block metadata
boolean pushBlockStartsToServer(Map<String, Long> blockStarts);   // Handle timing data
boolean pushBlockEndsToServer(Map<String, String> blockEnds);     // Handle completion data
```

**Data Flow**:
- Mixed log types in single list (Log and SubBlockStartLog instances)
- Block metadata separate from log entries
- Timing data extracted for performance analysis

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

```json
[
  {
    "block_id": "b1",
    "parent_block_id": null,
    "name": "root_block_wowo",
    "start_time": "some time formatted nicely as string with precision. (Based on flushBlockStart() method's map's value which is utc milisecond time)",
    "end_time": "time formated nicely with precision (based on current utc milisecond time)",
    "end_message": "null (root blocks dont have end message)(based on flushLogEndMethod)",
    "logs_chain": [
      {
        "id": "logID1",
        "type": "MESSAGE",
        "mesage": "STARTING BLOCK ",
        "logs_chain": [
          {
            "id": "logID1",
            "type": "MESSAGE",
            "mesage": "STARTING BLOCK "
          },
          {
            "id": "log2",
            "type": "SUB_BLOCK_START_SECONDARY",
            "end_message": "null (root blocks dont have end message)(based on flushLogEndMethod)",
            "duration": "start time - end time delta",
            "referenced_block": {
              "block_id": "b1",
              "parent_block_id": null,
              "name": "root_block_wowo",
              "start_time": "some time formatted nicely as string with precision. (Based on flushBlockStart() method's map's value which is utc milisecond time)",
              "end_time": "time formated nicely with precision (based on current utc milisecond time)",
              "end_message": "null (root blocks dont have end message)(based on flushLogEndMethod)",
              "logs_chain": [
                .....
              ]
            }
          }
        ]
      }
    ]
  }
]
```