# Java VFL Client API - User Documentation

## Table of Contents
1. [What is VFL?](#what-is-vfl)
2. [Installation & Setup](#installation--setup)
3. [VFL Variants Overview](#vfl-variants-overview)
4. [Recommended Approach: ThreadVFL](#recommended-approach-threadvfl)
5. [Fluent API with FluentThreadVFL](#fluent-api-with-fluentthreadvfl)
6. [Logging Methods Explained](#logging-methods-explained)
7. [Block Types & Flow Control](#block-types--flow-control)
8. [Alternative: PassVFL](#alternative-passvfl)
9. [Complete Examples](#complete-examples)
10. [Configuration Reference](#configuration-reference)

---

## What is VFL?

Visual Flow Logger (VFL) creates **hierarchical logs** that mirror your application's execution flow. Instead of flat, unstructured logs, VFL organizes your logs into meaningful blocks that show:

- **Method execution flow** - See which methods call which
- **Parallel operations** - Track concurrent executions  
- **Timing relationships** - Understand execution order
- **Context preservation** - Maintain scope across complex operations

**Traditional Logging:**
```
[INFO] Starting user registration
[INFO] Validating email
[INFO] Email is valid
[INFO] Checking email availability  
[INFO] Email available
[INFO] Hashing password
[INFO] Password hashed
[INFO] Saving user
[INFO] User saved with ID: 123
```

**VFL Hierarchical Logging:**
```
📁 User Registration Flow
  ├── 📝 Starting user registration  
  ├── 📁 Validation Block
  │   ├── 📝 Validating email format
  │   └── 📝 Email format is valid
  ├── 📁 Parallel Operations
  │   ├── 📁 Email Check (async)
  │   │   ├── 📝 Querying database for email
  │   │   └── 📝 Email is available
  │   └── 📁 Password Hashing (async)  
  │       ├── 📝 Generating salt
  │       └── 📝 Password hashed successfully
  ├── 📁 User Creation
  │   ├── 📝 Creating user record
  │   └── 📝 User saved with ID: 123
  └── 📝 Registration completed successfully
```

---

## Installation & Setup

### 1. Add Repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Add Dependency
```xml
<dependency>
    <groupId>com.github.kuchuk-borom-debbarma</groupId>
    <artifactId>vfl_java</artifactId>
    <version>0.1.1-alpha</version>
</dependency>
```

### 3. Basic Setup
```java
// Create flush handler (where logs go)
ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();

// Create buffer (manages log storage and flushing)
VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(
    100,                              // Buffer size
    5000,                            // Flush every 5 seconds
    flushHandler,                    // Where to send logs
    Executors.newFixedThreadPool(2)  // Thread pool for async ops
);
```

---

## VFL Variants Overview

VFL provides four main approaches for different use cases:

| Variant | Best For | Context Management | Complexity | Notes |
|---------|----------|-------------------|------------|-------|
| **ThreadVFL** ⭐ | Most applications | Automatic (ThreadLocal) | Simple | Static methods, automatic context |
| **FluentThreadVFL** ⭐ | Expressive, readable code | Automatic (ThreadLocal) | Simple | Built on ThreadVFL, fluent syntax |
| **PassVFL** | Legacy/specific needs | Manual passing | Complex | Manual logger passing |
| **FluentVFL** | PassVFL with fluent API | Manual passing | Complex | Fluent wrapper for any VFL instance |

### **Recommended: ThreadVFL + FluentThreadVFL**
- **Automatic context management** - No need to pass loggers around
- **Thread-safe** - Works seamlessly in multi-threaded applications  
- **Clean syntax** - Static methods, fluent API
- **Zero boilerplate** - Just start logging

### **FluentVFL - Universal Fluent Wrapper**
- **Works with any VFL instance** - Can wrap PassVFL, ThreadVFL, or any VFL implementation
- **Manual instantiation required** - Must create new instance for each block/context
- **Flexible** - Provides fluent API for PassVFL users

---

## Recommended Approach: ThreadVFL

ThreadVFL uses ThreadLocal storage to automatically manage logging context. You never need to pass logger instances around - just use static methods anywhere in your code.

### Basic Structure
```java
ThreadVFL.Runner.Instance.StartVFL("Root Block Name", buffer, () -> {
    // Your application logic with VFL logging
    ThreadVFL.Log("This automatically knows which block it belongs to");
    return null; // or return your result
});
```

### Simple Logging Example
```java
ThreadVFL.Runner.Instance.StartVFL("User Service", buffer, () -> {
    ThreadVFL.Log("Starting user service operation");
    ThreadVFL.Warn("This is a warning message");
    ThreadVFL.Error("This is an error message");
    
    String result = "operation_completed";
    ThreadVFL.Log("Service completed with result: {}", result);
    return result;
});
```

### Hierarchical Blocks
```java
ThreadVFL.Runner.Instance.StartVFL("Main Process", buffer, () -> {
    ThreadVFL.Log("Main process started");
    
    // Sequential sub-block
    String processedData = ThreadVFL.CallPrimarySubBlock(
        "Data Processing",              // Block name
        "Starting data processing",     // Start message  
        () -> {
            ThreadVFL.Log("Loading raw data");
            ThreadVFL.Log("Validating data format");
            ThreadVFL.Log("Transforming data");
            return "processed_data_v1";
        },
        result -> "Processing completed: " + result  // End message
    );
    
    ThreadVFL.Log("Main process completed with: {}", processedData);
    return null;
});
```

---

## Fluent API with FluentThreadVFL

FluentThreadVFL provides a more expressive, chainable API for complex operations. It's built on top of ThreadVFL and shares the same automatic context management.

### Basic Fluent Logging
```java
ThreadVFL.Runner.Instance.StartVFL("Fluent Example", buffer, () -> {
    // Fluent logging with parameter substitution
    FluentThreadVFL.Log("Processing user {} with email {}", "john_doe", "john@example.com");
    FluentThreadVFL.Warn("Memory usage is {}% - consider optimization", 85);
    FluentThreadVFL.Error("Failed to connect to external service");
    
    return null;
});
```

### Fluent Function Calls
```java
ThreadVFL.Runner.Instance.StartVFL("Calculation Service", buffer, () -> {
    // Execute function and log result
    int result = FluentThreadVFL.Call(() -> calculateSomething(10, 20))
        .asLog(value -> "Calculation result: " + value);
    
    // Execute with warning level
    String status = FluentThreadVFL.Call(() -> checkSystemHealth())
        .asWarn(health -> "System health: " + health);
        
    return result;
});
```

### Fluent Sub-blocks
```java
ThreadVFL.Runner.Instance.StartVFL("Order Processing", buffer, () -> {
    FluentThreadVFL.Log("Starting order processing");
    
    // Primary sub-block (sequential)
    Order validatedOrder = FluentThreadVFL.Call(() -> validateOrder(order))
        .asSubBlock("Order Validation")
        .withStartMessage("Validating order for customer: " + order.getCustomerId())
        .withEndMessageMapper(o -> "Validation completed - Order total: $" + o.getTotal())
        .startPrimary();
    
    // Runnable sub-block
    FluentThreadVFL.RunSubBlock(() -> sendConfirmationEmail(validatedOrder))
        .withBlockName("Email Notification")  
        .startPrimary();
        
    FluentThreadVFL.Log("Order processing completed");
    return validatedOrder;
});
```

---

## Logging Methods Explained

### Basic Logging Methods

#### `Log()` - Information Level
Use for general information about application flow:
```java
ThreadVFL.Log("User authentication successful");
ThreadVFL.Log("Processing {} records", recordCount);

// Fluent version with parameter substitution
FluentThreadVFL.Log("User {} logged in from IP {}", username, ipAddress);
```

#### `Warn()` - Warning Level  
Use for concerning situations that don't stop execution:
```java
ThreadVFL.Warn("High memory usage detected: 85%");
ThreadVFL.Warn("Retry attempt {} of {}", currentAttempt, maxAttempts);

// Fluent version
FluentThreadVFL.Warn("Database connection pool is {}% full", utilization);
```

#### `Error()` - Error Level
Use for error conditions and exceptions:
```java
ThreadVFL.Error("Failed to connect to payment gateway");
ThreadVFL.Error("Invalid user input: " + validationResult.getErrors());

// Fluent version  
FluentThreadVFL.Error("Critical system error occurred");
```

### Function Logging Methods

These methods execute functions and automatically log their results:

#### `LogFn()` - Execute and Log Result
```java
// Execute function and log result at INFO level
String result = ThreadVFL.LogFn(
    () -> processPayment(paymentRequest),           // Function to execute
    payment -> "Payment processed: " + payment.getId()  // How to format result
);

// Fluent version
String result = FluentThreadVFL.Call(() -> processPayment(paymentRequest))
    .asLog(payment -> "Payment processed: " + payment.getId());
```

#### `WarnFn()` - Execute and Log Result as Warning
```java
// Execute function and log result at WARN level
Integer connectionCount = ThreadVFL.WarnFn(
    () -> database.getActiveConnectionCount(),
    count -> "Active DB connections: " + count + " (threshold: 50)",
    maxConnections  // Additional formatting parameters
);

// Fluent version
Integer count = FluentThreadVFL.Call(() -> database.getActiveConnectionCount())
    .asWarn(c -> "High connection usage: " + c);
```

#### `ErrorFn()` - Execute and Log Result as Error  
```java
// Execute function and log result at ERROR level (useful for error recovery)
String fallbackResult = ThreadVFL.ErrorFn(
    () -> getFallbackData(),
    data -> "Using fallback data: " + data.getSource()
);

// Fluent version
String fallback = FluentThreadVFL.Call(() -> getFallbackData())
    .asError(data -> "Fallback activated: " + data.getSource());
```

---

## Block Types & Flow Control

### Primary Sub-blocks (Sequential)
**When to use**: Sequential operations that are part of the main flow

```java
// ThreadVFL
String result = ThreadVFL.CallPrimarySubBlock(
    "Database Operation",                    // Block name
    "Querying user database",               // Start message
    () -> {
        ThreadVFL.Log("Connecting to database");
        ThreadVFL.Log("Executing query: SELECT * FROM users");
        return userRepository.findAll();
    },
    users -> "Found " + users.size() + " users"  // End message
);

// FluentThreadVFL  
List<User> users = FluentThreadVFL.Call(() -> userRepository.findAll())
    .asSubBlock("Database Query")
    .withStartMessage("Fetching all users from database")
    .withEndMessageMapper(userList -> "Retrieved " + userList.size() + " users")
    .startPrimary();
```

### Secondary Joining Blocks (Parallel)
**When to use**: Operations that run in parallel but need to join back to main flow

```java
ThreadVFL.Runner.Instance.StartVFL("Parallel Data Processing", buffer, () -> {
    ThreadVFL.Log("Starting parallel operations");
    
    // Both operations run in parallel
    CompletableFuture<String> userData = ThreadVFL.CallSecondaryJoiningBlock(
        "User Data Fetch",
        "Fetching user profile data", 
        () -> {
            ThreadVFL.Log("Calling user service API");
            Thread.sleep(1000); // Simulate API call
            return "user_profile_data";
        },
        data -> "User data retrieved: " + data
    );
    
    CompletableFuture<String> preferences = ThreadVFL.CallSecondaryJoiningBlock(
        "Preferences Fetch", 
        "Fetching user preferences",
        () -> {
            ThreadVFL.Log("Querying preferences database");
            Thread.sleep(800); // Simulate DB query
            return "user_preferences"; 
        },
        prefs -> "Preferences loaded: " + prefs,
        Executors.newVirtualThreadPerTaskExecutor() // Custom executor
    );
    
    // Wait for both to complete (they ran in parallel)
    String user = userData.get();
    String prefs = preferences.get();
    
    ThreadVFL.Log("Both operations completed - User: {}, Prefs: {}", user, prefs);
    return null;
});

// FluentThreadVFL version
CompletableFuture<String> asyncResult = FluentThreadVFL.Call(() -> expensiveOperation())
    .asSubBlock("Expensive Operation")  
    .withStartMessage("Starting expensive async operation")
    .withEndMessageMapper(result -> "Operation completed with: " + result)
    .startSecondaryJoining(customExecutor);
```

### Secondary Non-Joining Blocks (Fire and Forget)
**When to use**: Background operations that don't need to join back (logging, notifications, cleanup)

```java
// ThreadVFL
ThreadVFL.CallSecondaryNonJoiningBlock(
    "Audit Logging",
    "Recording user action for audit", 
    () -> {
        ThreadVFL.Log("Preparing audit log entry");
        ThreadVFL.Log("Writing to audit database");
        auditService.logUserAction(userAction);
    },
    Executors.newSingleThreadExecutor()
);

// FluentThreadVFL
FluentThreadVFL.RunSubBlock(() -> {
        cleanupTempFiles();
        notifyAdministrators();
    })
    .withBlockName("Background Cleanup")
    .startSecondaryNonJoining(backgroundExecutor);
```

### Event Publisher/Listener Pattern
**When to use**: Event-driven architectures, pub/sub patterns

```java
ThreadVFL.Runner.Instance.StartVFL("Event-Driven Process", buffer, () -> {
    ThreadVFL.Log("Starting event-driven workflow");
    
    // Create event publisher
    EventPublisherBlock orderEvent = ThreadVFL.CreateEventPublisherBlock(
        "Order Placed Event",
        "Order placed by customer: customer_123"
    );
    
    // Multiple listeners handle the event
    processInventoryUpdate(orderEvent);
    sendOrderConfirmation(orderEvent); 
    updateCustomerRewards(orderEvent);
    
    ThreadVFL.Log("Event processing initiated");
    return null;
});

// Event listener implementation  
private void processInventoryUpdate(EventPublisherBlock event) {
    ThreadVFL.Runner.Instance.StartEventListenerLogger(
        "Inventory Update",           // Listener name
        "Updating inventory levels",  // Start message
        buffer,                       // Same buffer
        event,                        // Event to listen to
        () -> {
            ThreadVFL.Log("Checking current inventory levels");
            ThreadVFL.Log("Updating stock quantities");
            ThreadVFL.Log("Inventory update completed");
        }
    );
}
```

---

## Alternative: PassVFL

PassVFL requires manually passing logger instances but gives you complete control over the logging context. Use this when:
- You need explicit control over logger lifecycle
- Working with legacy systems that can't use ThreadLocal
- Building frameworks or libraries

### Basic PassVFL Usage
```java
PassVFL.Runner runner = new PassVFL.Runner();

runner.StartVFL("Manual Context Example", buffer, (rootLogger) -> {
    rootLogger.log("Starting with root logger");
    
    // Primary sub-block - logger is passed automatically
    String result = rootLogger.callPrimarySubBlock(
        "Processing Block",
        "Starting data processing",
        (subLogger) -> {
            subLogger.log("Using sub-block logger");
            subLogger.warn("This is within the sub-block context");
            return "processed_data";
        },
        r -> "Processing completed: " + r
    );
    
    rootLogger.log("Main process completed: " + result);
    return result;
});
```

### PassVFL with Manual Context Management
```java
// When you need to pass loggers to other methods
private void processUserData(PassVFL logger, UserData data) {
    logger.log("Processing user: " + data.getId());
    
    // Create sub-block and pass logger to another method
    String result = logger.callPrimarySubBlock(
        "Validation",
        "Validating user data",  
        (validationLogger) -> {
            return validateUser(validationLogger, data);
        },
        r -> "Validation result: " + r
    );
    
    logger.log("User processing completed: " + result);
}

private String validateUser(PassVFL logger, UserData data) {
    logger.log("Checking user email format");
    logger.log("Verifying user age");
    return "valid";
}
```

## FluentVFL - Universal Fluent Wrapper

FluentVFL is a universal fluent wrapper that can work with **any VFL instance**. Unlike FluentThreadVFL which is tied to ThreadLocal context, FluentVFL must be manually instantiated for each logger instance.

### Key Characteristics:
- **Manual instantiation required** - Create new FluentVFL for each VFL instance
- **Works with any VFL** - PassVFL, ThreadVFL, or custom VFL implementations
- **Block-scoped** - Each sub-block needs its own FluentVFL instance
- **Flexible** - Provides fluent API capabilities to any VFL variant

### FluentVFL with PassVFL
```java
PassVFL.Runner runner = new PassVFL.Runner();

runner.StartVFL("FluentVFL Example", buffer, (rootLogger) -> {
    // Create FluentVFL wrapper for root logger
    FluentVFL rootFluent = new FluentVFL(rootLogger);
    
    rootFluent.log("Starting with fluent PassVFL wrapper");
    rootFluent.warn("This uses fluent syntax with PassVFL backend");
    
    // Execute function with fluent API
    String result = rootFluent.call(() -> "processed_data")
        .asLog(data -> "Processing completed: " + data);
    
    // Primary sub-block with fluent API
    String processedResult = rootLogger.callPrimarySubBlock(
        "Fluent Processing",
        "Starting fluent processing block",
        (subLogger) -> {
            // Create new FluentVFL for sub-block logger
            FluentVFL subFluent = new FluentVFL(subLogger);
            
            subFluent.log("Using fluent API in sub-block with {} parameter", result);
            
            return subFluent.call(() -> processData(result))
                .asSubBlock("Data Processing")
                .withStartMessage("Processing data: " + result)
                .withEndMessageMapper(r -> "Data processed successfully: " + r)
                .startPrimary();
        },
        r -> "Fluent processing completed: " + r
    );
    
    rootFluent.log("All processing completed with result: {}", processedResult);
    return processedResult;
});
```

### FluentVFL Method Chaining
```java
// With PassVFL instance
PassVFL passLogger = getPassVFLInstance();
FluentVFL fluent = new FluentVFL(passLogger);

// Fluent logging with parameter substitution
fluent.log("Processing user {} with status {}", userId, status);
fluent.warn("Memory usage at {}% - threshold is {}%", usage, threshold);

// Fluent function calls
String result = fluent.call(() -> calculateSomething())
    .asLog(value -> "Calculation result: " + value);

// Fluent sub-blocks (creates new sub-logger automatically)
Order order = fluent.call(() -> processOrder(orderRequest))
    .asSubBlock("Order Processing")
    .withStartMessage("Processing order for customer: " + customerId)
    .withEndMessageMapper(o -> "Order processed with ID: " + o.getId())
    .startPrimary();

// Runnable sub-blocks
fluent.runSubBlock(() -> sendNotification(order))
    .withBlockName("Notification Service")
    .startPrimary();
```

### When to Use FluentVFL vs FluentThreadVFL

#### Use FluentThreadVFL when:
- Using ThreadVFL (recommended approach)
- Want automatic context management
- Building new applications
- Need static method convenience

```java
// FluentThreadVFL - automatic context, static methods
ThreadVFL.Runner.Instance.StartVFL("Example", buffer, () -> {
    FluentThreadVFL.Log("Automatic context management");
    
    String result = FluentThreadVFL.Call(() -> doSomething())
        .asSubBlock("Processing")
        .startPrimary();
    
    return result;
});
```

#### Use FluentVFL when:
- Using PassVFL or custom VFL implementations
- Need explicit control over logger instances
- Working with legacy systems
- Building frameworks that need flexibility

```java
// FluentVFL - manual instantiation, explicit control
PassVFL.Runner runner = new PassVFL.Runner();
runner.StartVFL("Example", buffer, (passLogger) -> {
    FluentVFL fluent = new FluentVFL(passLogger);  // Manual creation
    fluent.log("Explicit logger management");
    
    String result = passLogger.callPrimarySubBlock("Processing", "Starting", (subLogger) -> {
        FluentVFL subFluent = new FluentVFL(subLogger);  // New instance for sub-block
        return subFluent.call(() -> doSomething())
            .asLog(r -> "Result: " + r);
    }, r -> "Completed: " + r);
    
    return result;
});
```

### FluentVFL Limitations
- **Manual management** - Must create new instance for each logger
- **Memory overhead** - Additional wrapper objects
- **Complexity** - More verbose than FluentThreadVFL
- **Block scope** - Each sub-block needs new FluentVFL instance

**Recommendation**: Use FluentThreadVFL unless you specifically need the flexibility of PassVFL with fluent syntax.

---

## Complete Examples

### Web Service Example
```java
@Service
public class OrderService {
    private final VFLBuffer buffer;
    
    public OrderService() {
        ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();
        this.buffer = new ThreadSafeAsyncVFLBuffer(200, 3000, flushHandler,
            Executors.newFixedThreadPool(4));
    }
    
    public Order processOrder(CreateOrderRequest request) {
        return ThreadVFL.Runner.Instance.StartVFL("Process Order", buffer, () -> {
            FluentThreadVFL.Log("Processing order for customer: {}", request.getCustomerId());
            
            // Sequential validation
            OrderValidation validation = FluentThreadVFL.Call(() -> validateOrder(request))
                .asSubBlock("Order Validation")
                .withStartMessage("Validating order details and customer")
                .withEndMessageMapper(v -> "Validation completed - Status: " + v.getStatus())
                .startPrimary();
                
            if (!validation.isValid()) {
                FluentThreadVFL.Error("Order validation failed: {}", validation.getErrors());
                throw new InvalidOrderException(validation.getErrors());
            }
            
            // Parallel operations
            CompletableFuture<InventoryCheck> inventoryFuture = FluentThreadVFL.Call(() -> checkInventory(request))
                .asSubBlock("Inventory Check")
                .withStartMessage("Checking product availability")  
                .withEndMessageMapper(check -> "Inventory status: " + check.getStatus())
                .startSecondaryJoining(null);
                
            CompletableFuture<PaymentAuth> paymentFuture = FluentThreadVFL.Call(() -> authorizePayment(request))
                .asSubBlock("Payment Authorization")
                .withStartMessage("Authorizing payment method")
                .withEndMessageMapper(auth -> "Payment authorized: " + auth.getTransactionId())
                .startSecondaryJoining(Executors.newVirtualThreadPerTaskExecutor());
            
            // Wait for parallel operations
            InventoryCheck inventory = inventoryFuture.get();
            PaymentAuth payment = paymentFuture.get();
            
            if (!inventory.isAvailable()) {
                FluentThreadVFL.Error("Insufficient inventory for order");
                throw new InsufficientInventoryException();
            }
            
            // Create order
            Order order = FluentThreadVFL.Call(() -> createOrderRecord(request, payment))
                .asSubBlock("Order Creation")
                .withStartMessage("Creating order record in database")
                .withEndMessageMapper(o -> "Order created with ID: " + o.getId())
                .startPrimary();
            
            // Fire-and-forget operations
            FluentThreadVFL.RunSubBlock(() -> sendOrderConfirmation(order))
                .withBlockName("Order Confirmation Email")
                .startSecondaryNonJoining(null);
                
            FluentThreadVFL.RunSubBlock(() -> updateInventoryLevels(request))
                .withBlockName("Inventory Update")
                .startSecondaryNonJoining(null);
            
            FluentThreadVFL.Log("Order processing completed successfully - Order ID: {}", order.getId());
            return order;
        });
    }
    
    private OrderValidation validateOrder(CreateOrderRequest request) {
        FluentThreadVFL.Log("Validating customer ID: {}", request.getCustomerId());
        FluentThreadVFL.Log("Validating {} order items", request.getItems().size());
        FluentThreadVFL.Log("Validating payment method: {}", request.getPaymentMethod());
        return new OrderValidation(true, "Valid");
    }
    
    private InventoryCheck checkInventory(CreateOrderRequest request) {
        FluentThreadVFL.Log("Checking inventory for {} items", request.getItems().size());
        // Simulate inventory check
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        FluentThreadVFL.Log("All items are available in stock");
        return new InventoryCheck(true, "Available");
    }
    
    private PaymentAuth authorizePayment(CreateOrderRequest request) {
        FluentThreadVFL.Log("Contacting payment gateway");
        FluentThreadVFL.Log("Authorizing payment of ${}", request.getTotalAmount());
        // Simulate payment authorization
        try { Thread.sleep(800); } catch (InterruptedException e) {}
        String transactionId = "TXN_" + System.currentTimeMillis();
        FluentThreadVFL.Log("Payment authorized with transaction ID: {}", transactionId);
        return new PaymentAuth(true, transactionId);
    }
}
```

### Event-Driven Microservice Example
```java
@Component
public class UserRegistrationService {
    
    public void handleUserRegistration(UserRegisteredEvent event) {
        ThreadVFL.Runner.Instance.StartVFL("User Registration Handler", buffer, () -> {
            FluentThreadVFL.Log("Handling user registration for: {}", event.getEmail());
            
            // Create event for other services
            EventPublisherBlock welcomeEvent = ThreadVFL.CreateEventPublisherBlock(
                "New User Welcome",
                "New user registered: " + event.getEmail()
            );
            
            // Multiple services handle the welcome event
            setupUserProfile(welcomeEvent, event);
            sendWelcomeEmail(welcomeEvent, event);  
            createUserPreferences(welcomeEvent, event);
            recordAnalytics(welcomeEvent, event);
            
            FluentThreadVFL.Log("User registration handling completed");
            return null;
        });
    }
    
    private void setupUserProfile(EventPublisherBlock event, UserRegisteredEvent userEvent) {
        ThreadVFL.Runner.Instance.StartEventListenerLogger(
            "Profile Setup",
            "Setting up user profile",
            buffer,
            event,
            () -> {
                FluentThreadVFL.Log("Creating default user profile");
                FluentThreadVFL.Log("Setting up account permissions");
                FluentThreadVFL.Log("Profile setup completed for: {}", userEvent.getEmail());
            }
        );
    }
    
    private void sendWelcomeEmail(EventPublisherBlock event, UserRegisteredEvent userEvent) {
        ThreadVFL.Runner.Instance.StartEventListenerLogger(
            "Welcome Email",
            "Sending welcome email",
            buffer, 
            event,
            () -> {
                FluentThreadVFL.Log("Loading email template");
                FluentThreadVFL.Log("Personalizing content for: {}", userEvent.getEmail());
                FluentThreadVFL.Log("Sending email via SMTP");
                FluentThreadVFL.Log("Welcome email sent successfully");
            }
        );
    }
}
```

---

## Configuration Reference

### Buffer Types

#### ThreadSafeAsyncVFLBuffer (Recommended)
```java
VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(
    bufferSize,      // Number of log entries before forced flush (e.g., 100-500)
    flushInterval,   // Milliseconds between periodic flushes (e.g., 1000-10000)  
    flushHandler,    // Where to send logs
    executor         // Thread pool for async operations
);
```

**Best for**: High-throughput applications, production systems
**Features**: Non-blocking, periodic flushing, better performance

#### ThreadSafeSynchronousVflBuffer
```java
VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(
    bufferSize,      // Number of log entries before forced flush
    flushHandler     // Where to send logs
);
```

**Best for**: Simple applications, testing, debugging
**Features**: Simpler implementation, immediate consistency

### Flush Handlers

#### InMemoryFlushHandler (Development/Testing)
```java
ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();

// Later, get structured JSON output:
String jsonOutput = flushHandler.generateNestedJsonStructure();
System.out.println(jsonOutput);
```

#### VFL Hub (Production - Recommended for Distributed Systems)
```java
// Configure to send logs to VFL Hub server
VFLFlushHandler flushHandler = new VFLHubFlushHandler("http://your-vfl-hub:8080");
```

**Important**: For microservices and distributed systems, always use VFL Hub to get a unified view of logs across all services.

### Recommended Configurations

#### Development/Testing
```java
ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();
VFLBuffer buffer = new ThreadSafeSynchronousVflBuffer(50, flushHandler);
```

#### Production (Single Service)  
```java
ThreadSafeInMemoryFlushHandlerImpl flushHandler = new ThreadSafeInMemoryFlushHandlerImpl();
VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(200, 5000, flushHandler, 
    Executors.newFixedThreadPool(3));
```

#### Production (Distributed Systems)
```java
VFLFlushHandler hubHandler = new VFLHubFlushHandler("http://vfl-hub.company.com:8080");
VFLBuffer buffer = new ThreadSafeAsyncVFLBuffer(300, 3000, hubHandler,
    Executors.newFixedThreadPool(5));
```

### Cleanup  
Always ensure proper cleanup:
```java
try {
    // Your VFL operations
} finally {
    buffer.flushAndClose();
}
```

---

## Quick Reference

### ThreadVFL Static Methods
```java
// Basic logging
ThreadVFL.Log("message");
ThreadVFL.Warn("warning");  
ThreadVFL.Error("error");

// Function logging
ThreadVFL.LogFn(() -> function(), result -> "msg");
ThreadVFL.WarnFn(() -> function(), result -> "msg");
ThreadVFL.ErrorFn(() -> function(), result -> "msg");

// Block operations
ThreadVFL.CallPrimarySubBlock("name", "start", () -> code, result -> "end");
ThreadVFL.CallSecondaryJoiningBlock("name", "start", () -> code, executor, result -> "end");
ThreadVFL.CallSecondaryNonJoiningBlock("name", "start", () -> code, executor);
ThreadVFL.CreateEventPublisherBlock("name", "message");
```

### FluentThreadVFL Static Methods  
```java
// Fluent logging
FluentThreadVFL.Log("message with {} and {}", param1, param2);
FluentThreadVFL.Warn("warning with {}", param);
FluentThreadVFL.Error("error message");

// Fluent calls
FluentThreadVFL.Call(() -> function()).asLog(r -> "message");
FluentThreadVFL.Call(() -> function()).asSubBlock("name").withStartMessage("start").startPrimary();
FluentThreadVFL.RunSubBlock(() -> runnable()).withBlockName("name").startPrimary();
```

**Remember**: Always use `ThreadVFL.Runner.Instance.StartVFL()` to begin your VFL logging session!
