# Visual Flow Logger (VFL)

## Introduction

**Visual Flow Logger (VFL)** is a **Structured Logging Framework** designed to capture and visualize how your applications actually execute. Unlike traditional flat logging that gives you disconnected log entries, VFL creates a hierarchical representation of your program's flow that shows the relationships between different operations and how they unfold over time.

VFL supports **distributed tracing**, meaning the structured flow can seamlessly span multiple services and systems, giving you a complete picture of complex workflows across your entire architecture.

## Getting Started

### 1. Set up VFLHub

First, you'll need to host VFLHub - the central server that stores and visualizes your logs.

**[VFLHub Setup Guide →](https://github.com/vfl/vflhub)**

VFLHub provides:
- Log storage and processing
- Web-based visualization interface
- API endpoints for log ingestion

### 2. Integrate VFL Client Framework

Add the VFL Java Client framework to your project to start structured logging.

**[Java VFL Client Documentation →](https://github.com/vfl/java-client)**

The client framework handles:
- Hierarchical log creation
- Automatic context management
- Transmission to VFLHub
- Cross-service trace correlation

## Why VFL?

Traditional logging gives you this:
```
[INFO] Processing user order
[INFO] Validating payment method
[INFO] Checking inventory 
[INFO] Payment processed
[INFO] Inventory updated
[INFO] Order completed
```

VFL gives you this structured view:
```
📁 Process User Order
  ├── 📝 Starting order processing
  ├── 📁 Payment Processing (parallel)
  │   ├── 📝 Validating payment method
  │   ├── 📝 Contacting payment gateway
  │   └── 📝 Payment authorized
  ├── 📁 Inventory Check (parallel)
  │   ├── 📝 Checking product availability
  │   └── 📝 Stock confirmed
  ├── 📝 Creating order record
  └── 📝 Order #12345 completed
```

## Features

### 1. Hierarchical Logging

VFL structures your logs in a tree-like hierarchy, making it easy to understand the relationship between different parts of your program execution.

### 2. Multiple Flow Patterns

VFL can represent the full spectrum of modern application execution patterns:

- **Sequential Flow** - Step-by-step operations that happen one after another
- **Parallel Operations** - Multiple operations running simultaneously
- **Fire-and-Forget** - Background tasks that don't need to report back
- **Rejoining Parallel** - Parallel operations that eventually merge back into the main flow
- **Event-Driven** - Publisher/subscriber patterns and event-driven architectures
- **Cross-Service Flows** - Operations that span multiple microservices or distributed systems

### 3. Distributed Tracing

VFL's structured approach naturally extends across service boundaries. When Service A calls Service B, VFL maintains the hierarchical structure, showing you:

- How requests flow between services
- Timing relationships across your architecture
- The complete end-to-end journey of complex operations
- Dependencies and interactions in distributed systems

## Design Philosophy

In Visual Flow Logger, everything is represented as **Blocks** and **Logs**.

### Blocks

Blocks are containers that represent a scope of execution. Each block contains:

- **Unique identifier** for linking and referencing
- **Human-readable name** describing its purpose
- **Timestamps** showing when it started and ended
- **Hierarchical relationships** to parent and child blocks

Blocks represent whatever scope makes sense for your application - you decide what constitutes a meaningful boundary.

### Logs

Logs are the individual events that happen within blocks:

- Method entry and exit points
- Decision points and conditions
- Data transformations
- External service calls
- Error conditions
- Any other significant events

Logs are chronologically ordered within their block and linked to their containing block for context.

### Nested Structure

The power of VFL comes from how blocks and logs work together:

- **Blocks contain logs** showing step-by-step execution
- **Logs can reference other blocks**, creating hierarchical relationships
- **Child blocks** represent sub-operations or deeper detail levels
- **Parent blocks** provide context for understanding the bigger picture

This creates a natural tree structure that mirrors how applications actually execute - from high-level business operations down to detailed implementation steps.

## Examples

### Example 1: Simple Sequential Flow

```
User Registration Block
├── Log: "Starting user validation"
├── Email Validation Block
│   ├── Log: "Checking email format"
│   ├── Log: "Email format is valid"
│   ├── Log: "Checking if email already exists"
│   └── Log: "Email is available"
├── Password Processing Block
│   ├── Log: "Validating password strength"
│   ├── Log: "Password meets requirements"
│   ├── Log: "Generating password hash"
│   └── Log: "Password hashed successfully"
├── Database Operations Block
│   ├── Log: "Creating user record"
│   ├── Log: "Saving to users table"
│   └── Log: "User created with ID: 12345"
└── Log: "Registration completed successfully"
```

### Example 2: Parallel Operations with Rejoining

```
E-commerce Order Processing Block
├── Log: "Processing order #ORD-789"
├── Log: "Starting parallel validations"
├── Payment Authorization Block (⏱️ 1.2s)
│   ├── Log: "Validating credit card details"
│   ├── Log: "Contacting payment gateway"
│   ├── Log: "Payment authorized: $149.99"
│   └── Log: "Transaction ID: TXN-ABC123"
├── Inventory Check Block (⏱️ 0.8s)
│   ├── Log: "Checking product availability"
│   ├── Log: "Product SKU-001: 15 units available"
│   ├── Log: "Product SKU-002: 8 units available"
│   └── Log: "All items in stock - reserving inventory"
├── Log: "Both validations completed - proceeding"
├── Order Creation Block
│   ├── Log: "Generating order confirmation"
│   ├── Log: "Updating inventory levels"
│   └── Log: "Order record saved"
└── Log: "Order processing completed"
```

### Example 3: Fire-and-Forget Background Operations

```
User Profile Update Block
├── Log: "Updating profile for user: john_doe"
├── Profile Validation Block
│   ├── Log: "Validating profile fields"
│   ├── Warning: "Profile image size is large 2.5MB"
│   └── Log: "Validation passed"
├── Database Update Block
│   ├── Log: "Updating user_profiles table"
│   └── Log: "Profile updated successfully"
├── Cache Invalidation Block (🔥 Fire-and-Forget)
│   ├── Log: "Invalidating user cache"
│   └── Log: "Cache cleared for user: john_doe"
├── Analytics Tracking Block (🔥 Fire-and-Forget)
│   ├── Log: "Recording profile update event"
│   └── Log: "Event sent to analytics service"
├── Email Notification Block (🔥 Fire-and-Forget)
│   ├── Log: "Preparing profile update email"
│   ├── Log: "Sending notification email"
│   └── Log: "Email queued successfully"
└── Log: "Profile update completed - background tasks initiated"
```

### Example 4: Distributed Cross-Service Flow

```
API Gateway: User Authentication Block
├── Log: "Received login request for: john@example.com"
├── Log: "Routing to authentication service"
├── Auth Service: Validate Credentials Block (🌐 Cross-Service)
│   ├── Log: "Processing authentication request"
│   ├── Log: "Looking up user in database"
│   ├── Password Verification Block
│   │   ├── Log: "Retrieving stored password hash"
│   │   ├── Log: "Comparing provided password"
│   │   └── Log: "Password verification successful"
│   ├── Log: "Calling token service for JWT generation"
│   ├── Token Service: Generate JWT Block (🌐 Cross-Service)
│   │   ├── Log: "Generating JWT token for user: 12345"
│   │   ├── Log: "Setting token expiration: 24 hours"
│   │   ├── Log: "Signing token with private key"
│   │   └── Log: "Token generated successfully"
│   ├── Log: "Received JWT token from token service"
│   ├── Log: "Recording login event in audit log"
│   └── Log: "Authentication completed successfully"
├── Log: "Received successful auth response"
├── Log: "Adding security headers"
├── Log: "Returning JWT to client"
└── Log: "Authentication flow completed - total time: 245ms"
```

## The VFL Advantage

VFL transforms logging from a debugging afterthought into a powerful tool for:

- **Understanding complex systems** through clear visual structure
- **Debugging distributed applications** with end-to-end trace visibility
- **Performance analysis** with timing relationships preserved
- **System monitoring** with meaningful operational context
- **Team collaboration** through shared understanding of application flow

Whether you're building a simple application or managing a complex distributed system, VFL provides the structured visibility you need to understand, debug, and optimize your software.