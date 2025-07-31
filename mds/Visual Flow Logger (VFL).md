# Visual Flow Logger (VFL)



# Visual Flow Logger

## Introduction

**Visual Flow Logger (VFL)** is a **Hierarchical Logging Framework** that helps users create structured logs for visualizing and analyzing program flow. Unlike traditional flat logging, VFL organizes your logs into a meaningful hierarchy that mirrors how your application actually executes.

## Features

### 1. Hierarchical Logging

VFL structures your logs in a tree-like hierarchy, making it easy to understand the relationship between different parts of your program execution.

### 2. Flexible Flow Representation

VFL can represent various types of program flow:

* **Linear flow** - Sequential execution steps
* **Concurrent flow** - Parallel operations and multi-threading
* **Fire and Forget flow** - Asynchronous operations that don't block
* **Event publisher & listener** - Event-driven architectures

### 3. Easy-to-Use Logging API

VFL provides multiple integration approaches to fit your coding style:

* **Context Passing** - Pass context objects through your application
* **Scoped Logger** - Automatic scope management with try-with-resources pattern
* **Fluent API** - Chain method calls for readable logging code

### 4. Multiple Output Formats

Your logs can be processed and viewed in different ways:

* **VFL Server & Web Application** - Interactive web-based visualization
* **JSON** - Structured data format for custom processing
* **Mermaid code** - Generate flowcharts and diagrams

## Design Philosophy

In Visual Flow Logger, everything is represented as **Blocks** and **Logs**.

### Blocks

Blocks are containers that represent a scope of execution. Each block contains:

* **ID** - Unique identifier for referencing and linking
* **Name** - Human-readable description of the block's purpose
* **Start Timestamp** - When the block began executing
* **End Timestamp** - When the block completed (if finished)

Blocks represent whatever scope makes sense for your application - you decide what constitutes a meaningful boundary.

### Logs

Logs are the individual entries within blocks that capture what happened during execution. These logs are ordered chronologically, showing you the exact flow of operations within each block.

### Nested Structure

The power of VFL comes from how blocks and logs work together:

* **Blocks contain logs** that show the step-by-step execution
* **Logs can reference other blocks** through special log types, creating nested hierarchies
* **Child blocks** represent sub-operations or deeper levels of detail
* **Parent blocks** provide context for understanding the bigger picture

This nested structure naturally maps to how programs execute - from high-level operations down to detailed implementation steps - making it intuitive to navigate and debug your application flow.

## Examples

### Example 1: Simple Function Call

```
User Registration Block
├── Log: "Starting user validation"
├── Log: "Email format is valid"
├── Log: "Password meets requirements"
├── Log: "Validation complete"
├── Log: "Saving user to database"
└── Log: "User registered successfully"

```

In this example, the entire user registration process is one block, with each step logged chronologically.

### Example 2: Nested Operations

```
E-commerce Order Block
├── Log: "Processing order #12345"
├── Payment Processing Block
│   ├── Log: "Validating credit card"
│   ├── Log: "Contacting payment gateway"
│   ├── Log: "Payment authorized: $99.99"
│   └── Log: "Payment complete"
├── Log: "Payment successful, proceeding to fulfillment"
├── Inventory Update Block
│   ├── Log: "Checking item availability"
│   ├── Log: "Reserving 2 units of Product ABC"
│   └── Log: "Inventory updated"
└── Log: "Order #12345 completed successfully"

```

This shows how VFL can represent parallel operations, with multiple blocks executing simultaneously under a parent block.

# Off topic

[VFL User Documentation](file:///workspace/86f395c5-4710-426d-a3b2-15d41e296a0d/qbYZW_PC7qZCL0-gdQEVG)

[VFL Technical Documentation](file:///workspace/86f395c5-4710-426d-a3b2-15d41e296a0d/6MDywhNhah7R3rvRJkj_Z)
