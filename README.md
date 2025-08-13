# VFL Client Java Framework - User Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation & Setup](#installation--setup)
3. [Quick Start](#quick-start)
4. [Core Concepts](#core-concepts)
5. [Basic Usage](#basic-usage)
6. [Advanced Features](#advanced-features)
7. [Configuration Options](#configuration-options)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)
10. [API Reference](#api-reference)
11. [Examples](#examples)

---

## Introduction

The **VFL Client Java Framework** is a Java implementation of Visual Flow Logger (VFL) that enables structured, hierarchical logging in your Java applications. Unlike traditional flat logging systems, VFL creates a tree-like structure that shows how your application executes, making debugging, monitoring, and performance analysis easier.

### Key Features

- **Minimal Refactoring**: Add `@SubBlock` annotations to existing methods
- **Hierarchical Logging**: View your application flow as a tree structure
- **Distributed Tracing**: Track operations across multiple services
- **Asynchronous Support**: Handle parallel operations and background tasks
- **Event-Driven Patterns**: Support for pub/sub and event-driven architectures
- **Thread Safety**: Built for concurrent applications

### Integration Approach

VFL integrates into existing codebases with minimal changes:

```java
// Your existing method
public void processPayment(String orderId) {
    validatePayment(orderId);
    chargeCard(orderId);
}

// Add VFL tracing - minimal changes required
@SubBlock(blockName = "Process Payment {0}")
public void processPayment(String orderId) {
    validatePayment(orderId);  // Same code
    chargeCard(orderId);       // Same code  
}
```

---

## Installation & Setup

### Prerequisites

- Java 11 or higher
- Maven or Gradle build system
- **VFL Hub server running** (see [VFL Hub Setup Guide](https://github.com/vfl/vflhub))

### Maven Dependency

```xml
<dependency>
    <groupId>dev.kuku.vfl</groupId>
    <artifactId>vfl-client-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'dev.kuku.vfl:vfl-client-java:1.0.0'
```

---

## Quick Start

### 1. Initialize VFL

```java
import dev.kuku.vfl.impl.annotation.*;
import dev.kuku.vfl.core.buffer.AsyncBuffer;

public class Application {
    public static void main(String[] args) {
        // Initialize VFL
        VFLFlushHandler flushHandler = new VFLHubFlushHandler(
            URI.create("http://localhost:8080")
        );
        
        VFLBuffer buffer = new AsyncBuffer(100, 3000, 1000, flushHandler, 
            Executors.newVirtualThreadPerTaskExecutor(),
            Executors.newSingleThreadScheduledExecutor());
        
        VFLInitializer.initialize(new VFLAnnotationConfig(false, buffer));
        
        // Your application code
        new MyService().processOrder("ORD-12345");
    }
}
```

### 2. Add Tracing to Methods

```java
public class MyService {
    
    @SubBlock(blockName = "Process Order {0}")
    public void processOrder(String orderId) {
        Log.Info("Processing order {}", orderId);
        validateOrder(orderId);
        chargePayment(orderId);
    }
    
    @SubBlock(blockName = "Validate Order {0}")
    private void validateOrder(String orderId) {
        Log.Info("Validating order");
    }
    
    public void runExample() {
        VFLStarter.StartRootBlock("Order Processing", () -> {
            processOrder("ORD-12345");
        });
    }
}
```

### 3. View Your Logs

Open your VFL Hub dashboard at `http://localhost:8080` to view the structured flow visualization.

---

## Core Concepts

### Blocks

**Blocks** are containers that represent a scope of execution:

- **Root Block**: The top-level container for your entire operation
- **Sub-Block**: Child blocks created by `@SubBlock` annotations
- **Event Listener Block**: Blocks that handle asynchronous events
- **Continuation Block**: Blocks that continue a trace from another service

VFL automatically creates different types of blocks based on the context:

- **Primary Sub-Block**: Main execution path blocks created by `@SubBlock`
- **Secondary Sub-Block (No Join)**: Async operations that don't need to be waited for
- **Secondary Sub-Block (Join)**: Async operations that will be joined/awaited
- **Event Publisher**: Blocks that publish events for other services to consume
- **Event Listener**: Blocks that handle published events

### Logs

**Logs** are individual events that happen within blocks:

VFL supports three main types of logs:
- **Message**: Regular informational logs (`Log.Info()`)
- **Warning**: Warning messages that indicate potential issues (`Log.Warn()`)
- **Error**: Error messages for problems that need attention (`Log.Error()`)

### Hierarchical Structure

When you add `@SubBlock` annotations, VFL automatically creates this structure:

```
Root Block: "Process User Registration"
├── Log: "Starting registration process"
├── Sub-Block: "Validate Email" ← Created by @SubBlock
│   └── Log: "Email validation passed"
├── Sub-Block: "Create User Account" ← Created by @SubBlock
│   └── Log: "User account created"
└── Log: "Registration completed"
```

---

## Basic Usage

### Starting a Root Block

```java
// Simple root block
public void handleRequest() {
    VFLStarter.StartRootBlock("Handle Request", () -> {
        myService.processRequest();
    });
}

// Root block with return value
public String processOrder() {
    return VFLStarter.StartRootBlock("Process Order", () -> {
        return orderService.process();
    });
}
```

### Using @SubBlock Annotation

```java
@SubBlock(blockName = "Validate User {0}")
public boolean validateUser(String userId) {
    Log.Info("Validating user {}", userId);
    return checkPermissions(userId);
}
```

### Logging Within Blocks

```java
// Simple logging
Log.Info("Processing started");
Log.Warn("Low memory detected");
Log.Error("Database connection failed");

// With parameters
Log.Info("Processing user {} with status {}", userId, status);
```

---

## Advanced Features

### Asynchronous Operations

```java
public void processAsync() {
    VFLStarter.StartRootBlock("Async Processing", () -> {
        CompletableFuture<String> future = 
            VFLFutures.supplyAsync(() -> doWork());
        
        String result = future.join();
        Log.Info("Result: {}", result);
    });
}

@SubBlock
private String doWork() {
    Log.Info("Working...");
    return "Done";
}
```

### Event-Driven Patterns

```java
public void orderProcessing() {
    VFLStarter.StartRootBlock("Order Processing", () -> {
        // Publish an event
        EventPublisherBlock event = Log.Publish("Order Created", 
                                               "Order {} created", orderId);
        
        // Handle the event
        handleOrderEvent(event);
    });
}

private void handleOrderEvent(EventPublisherBlock event) {
    VFLStarter.StartEventListener(event, "Order Handler", () -> {
        Log.Info("Handling order event");
    });
}
```

### Distributed Tracing

```java
// Service A
public void serviceA() {
    VFLStarter.StartRootBlock("Service A", () -> {
        Log.CreateContinuationBlock("Call Service B", block -> {
            callServiceB(serialize(block));
        });
    });
}

// Service B  
public void serviceB(String serializedBlock) {
    Block block = deserialize(serializedBlock);
    VFLStarter.ContinueFromBlock(block, () -> {
        Log.Info("Continuing in Service B");
    });
}
```

---

## Configuration Options

### Buffer Types

```java
// Synchronous - flushes immediately
VFLBuffer buffer = new SynchronousBuffer(100, flushHandler);

// Asynchronous - flushes in background
VFLBuffer buffer = new AsyncBuffer(100, 3000, 1000, flushHandler, 
    executor, scheduledExecutor);

// No-Op - discards logs (testing)
VFLBuffer buffer = new NoOpsBuffer();
```

### Flush Handlers

```java
// VFL Hub (recommended for production)
VFLFlushHandler handler = new VFLHubFlushHandler(
    URI.create("http://localhost:8080"));

// File handler (testing only)
VFLFlushHandler handler = new NestedJsonFlushHandler("output.json");
```

---

## Best Practices

### 1. Root Block Placement

```java
// Correct
public void handleRequest() {
    VFLStarter.StartRootBlock("Handle Request", () -> {
        processRequest(); // @SubBlock methods work here
    });
}

// Incorrect - no parent context
@SubBlock  
public void processRequest() {
    // This will show a warning
}
```

### 2. Meaningful Block Names

```java
// Good
@SubBlock(blockName = "Validate Credit Card for Order {0}")

// Better  
@SubBlock(blockName = "Process Payment - Order {0}, Amount ${1}")

// Avoid
@SubBlock(blockName = "Method execution")
```

### 3. Use VFLFutures for Async

```java
// Correct - maintains trace context
CompletableFuture<String> future = VFLFutures.supplyAsync(() -> work());

// Incorrect - loses context
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> work());
```

---

## Troubleshooting

### Common Issues

#### 1. "No parent block" Warning
Ensure `@SubBlock` methods are called within a root block.

#### 2. Logs Not Appearing
- Check VFL Hub is running
- Verify network connectivity
- Check buffer flush settings

#### 3. Memory Issues
Reduce buffer size or flush interval.

---

## API Reference
[Complete API Reference](API_REFERENCE.MD)

### VFLStarter Class

| Method | Description |
|--------|-------------|
| `StartRootBlock(String, Runnable)` | Start new root block |
| `StartRootBlock(String, Supplier<T>)` | Start root block with return value |
| `ContinueFromBlock(Block, Runnable)` | Continue from existing block |
| `StartEventListener(EventPublisherBlock, String, Runnable)` | Start event listener |

### Log Class

| Method | Description |
|--------|-------------|
| `Info(String, Object...)` | Log info message |
| `Warn(String, Object...)` | Log warning |
| `Error(String, Object...)` | Log error |
| `Publish(String, String, Object...)` | Publish event |

### @SubBlock Annotation

| Attribute | Description |
|-----------|-------------|
| `blockName` | Block display name with {0}, {1} placeholders |
| `startMessage` | Optional message logged on entry |
| `endMessage` | Optional message logged on exit |

---

## Examples

### Example 1: Simple Order Processing

```java
public class OrderService {
    
    public void processOrder(String orderId) {
        VFLStarter.StartRootBlock("Process Order", () -> {
            validateOrder(orderId);
            chargePayment(orderId);
            createRecord(orderId);
        });
    }

    @SubBlock(blockName = "Validate Order {0}")
    private void validateOrder(String orderId) {
        Log.Info("Validating order {}", orderId);
    }

    @SubBlock(blockName = "Charge Payment {0}")
    private void chargePayment(String orderId) {
        Log.Info("Processing payment for {}", orderId);
    }

    @SubBlock(blockName = "Create Record {0}")
    private void createRecord(String orderId) {
        Log.Info("Creating order record for {}", orderId);
    }
}
```

### Example 2: Async Processing

```java
public class AsyncService {
    
    public void processAsync(String id) {
        VFLStarter.StartRootBlock("Async Processing", () -> {
            CompletableFuture<String> task1 = 
                VFLFutures.supplyAsync(() -> doTask1(id));
            
            CompletableFuture<String> task2 = 
                VFLFutures.supplyAsync(() -> doTask2(id));
            
            String result1 = task1.join();
            String result2 = task2.join();
            
            Log.Info("Results: {} | {}", result1, result2);
        });
    }

    @SubBlock
    private String doTask1(String id) {
        Log.Info("Executing task 1 for {}", id);
        return "Task1 complete";
    }

    @SubBlock  
    private String doTask2(String id) {
        Log.Info("Executing task 2 for {}", id);
        return "Task2 complete";
    }
}
```

### Example 3: Event-Driven Pattern

```java
public class EventService {
    
    public void processWithEvents(String orderId) {
        VFLStarter.StartRootBlock("Event-Driven Processing", () -> {
            Log.Info("Processing order {}", orderId);
            
            // Publish event
            EventPublisherBlock event = Log.Publish("Order Processed", 
                                                   "Order {} completed", orderId);
            
            // Handle event
            handleOrderComplete(event, orderId);
        });
    }

    private void handleOrderComplete(EventPublisherBlock event, String orderId) {
        VFLStarter.StartEventListener(event, "Order Complete Handler", () -> {
            sendEmail(orderId);
            updateInventory(orderId);
        });
    }

    @SubBlock
    private void sendEmail(String orderId) {
        Log.Info("Sending email for order {}", orderId);
    }

    @SubBlock
    private void updateInventory(String orderId) {
        Log.Info("Updating inventory for order {}", orderId);
    }
}
```

---

## Next Steps

1. **Set up VFL Hub**: Install and run the VFL Hub server
2. **Try the examples**: Start with the simple order processing example
3. **View the visualization**: Use VFL Hub's web interface to see your traces
4. **Explore advanced features**: Add async operations and events to your flows

---

*For more information, visit the [GitHub repository](https://github.com/vfl/java-client).*