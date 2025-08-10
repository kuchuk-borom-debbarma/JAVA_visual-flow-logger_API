# Visual Flow Logger (VFL) Project Context

## Project Overview
Visual Flow Logger (VFL) is a **Hierarchical Logging Framework** that helps users create structured logs for visualizing and analyzing program flow. Unlike traditional flat logging, VFL organizes logs into a meaningful hierarchy that mirrors application execution.

## Core Architecture Components

### 1. VFL Client API
- Main component that users interact with for logging
- Available in multiple programming languages (Java, Python, Node.js, .NET, Go)
- Provides different integration patterns: Context Passing, Scoped Logging, Fluent API

### 2. VFL Server/Hub (naming under consideration)
- Centralized server component (considering names like "VFL Hub", "VFL Central")
- Stores logs in database
- Provides web-based UI for visualization
- **Required for distributed systems/microservices**

## Key Concepts

### Blocks
- Containers representing execution scopes
- Have ID, Name, Start/End timestamps
- Can be nested to create hierarchical structures

### Logs
- Individual entries within blocks
- Chronologically ordered
- Can reference other blocks to create nested hierarchies

### Context Flow
- Automatic context management
- Maintains hierarchical relationships
- Preserves execution flow across services

## Configuration (VFL Client API)

### 1. Buffer
- Temporary storage for logs before flushing
- Types vary by language: synchronous, concurrent, asynchronous
- Memory vs persistent options available

### 2. FlushHandler
- Used internally by buffer to flush logs to output
- Primary VFLAnnotationConfig point for users
- Defines output destinations: VFL Hub, local files, custom outputs
- Configures flush policies (time-based, size-based, event-driven)

## Critical Architecture Decision
**Distributed Systems MUST use VFL Server/Hub** - Client-side output methods don't work properly in microservice architectures as each service creates separate logs with no unified view.

## Documentation Structure Created
1. **Main User Documentation Introduction** - Emphasizes VFL Server requirement for distributed systems
2. **VFL Client API Documentation** - Language-agnostic overview of concepts and patterns
3. Ready for language-specific implementations (Java Client API next)

## Flow Types Supported
- Linear flow (sequential execution)
- Concurrent flow (parallel operations)
- Fire and Forget flow (asynchronous operations)
- Event publisher & listener (event-driven architectures)

## Output Formats
- VFL Server & Web Application (recommended)
- JSON (structured data)
- Mermaid code (flowcharts and diagrams)

## Current Status
- User documentation introduction completed
- VFL Client API overview completed  
- Ready to proceed with Java Client API documentation
- Server naming still under consideration (VFL Server vs VFL Hub vs VFL Central)