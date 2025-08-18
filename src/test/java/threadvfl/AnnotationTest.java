package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.buffer.flushHandler.VFLHubFlushHandler;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.impl.annotation.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.Executors;

public class AnnotationTest {
    static VFLBuffer b;

    static VFLBuffer createBuffer(String fileName) {
        VFLFlushHandler f = new VFLHubFlushHandler(URI.create("http://localhost:8080"));
        return new AsyncBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void linear() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("linear")));
        new TestService().linear();
    }

    @Test
    void async() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("async")));
        new TestService().async();
    }

    @Test
    void eventListenerSyncTest() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("eventListenerSyncTest")));
        new TestService().eventPublisher();
    }

    @Test
    void complexRealWorldSimulation() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("complexSimulation")));
        new TestService().complexOrderProcessingSimulation();
    }


}

class TestService {
    @SubBlock(
            blockName = "block name is square {0}",
            startMessage = "squaring {0}",
            endMessage = "returned value is {r} for {0}"
    )
    private int square(int a) {
        return a * a;
    }

    @SubBlock
    private int squareAndMultiply(int a, int b) {
        int num = a * b;
        return square(num);
    }

    @SubBlock
    public void linear() {
        VFLStarter.StartRootBlock("Linear operation test", () -> {
            Log.Info("SUP");
            int a = Log.InfoFn(() -> square(12), "Squaring {} = {}", 12);
            int b = squareAndMultiply(a, 2);
            Log.Info("COMPLETE");
        });
    }

    public void async() {
        VFLStarter.StartRootBlock("Async operation test", () -> {
            Log.Info("Starting async test with thread pool");
            var e = Executors.newFixedThreadPool(1);
            var t = VFLFutures.runAsync(() -> {
                Log.Info("CRASH");
                square(1);
            });
            var t2 = VFLFutures.runAsync(() -> square(1), e);
            var y = VFLFutures.supplyAsync(() -> {
                Log.Info("Returning stuff");
                return square(2);
            }, e);

            t.join();
            t2.join();
            int num = y.join();
            Log.Info("COMPLETE with num {}", num);
        });
    }

    public void eventPublisher() {
        VFLStarter.StartRootBlock("Event publisher test", () -> {
            Log.Info("Starting event listener test");
            System.err.println("Debug - About to publish event");
            var p = Log.Publish("Ordered item");
            System.err.println("Debug - Published event, result: " + p);
            Log.Info("Published stuff");
            listenerOne(p);
            listenerOne(p);
            listenerOne(p);
            Log.Info("Another log after listener 1");
            listenerTwo(p);
            listenerTwo(p);
            listenerTwo(p);
        });
    }


    void listenerOne(EventPublisherBlock p) {
        VFLStarter.StartEventListener(p, "Listener 1", null, () -> {
            Log.Info("Listener 1");
            square(23);
            square(1);
            square(2);
        });
    }

    void listenerTwo(EventPublisherBlock p) {
        VFLStarter.StartEventListener(p, "Listener 2", null, () -> {
            Log.Info("Listener 2");
            square(21);
            square(1);
            square(8);
        });
    }

    public void complexOrderProcessingSimulation() {
        VFLStarter.StartRootBlock("E-Commerce Order Processing System", () -> {
            Log.Info("🚀 Starting order processing pipeline");

            // Simulate user authentication
            authenticateUser("john.doe@email.com");

            // Process multiple orders in parallel
            var executor = Executors.newFixedThreadPool(4);

            // Create order validation tasks
            var orderValidation1 = VFLFutures.supplyAsync(() -> validateOrder("ORD-001", 299.99), executor);
            var orderValidation2 = VFLFutures.supplyAsync(() -> validateOrder("ORD-002", 149.99), executor);
            var orderValidation3 = VFLFutures.supplyAsync(() -> validateOrder("ORD-003", 599.99), executor);

            // Parallel inventory checks
            var inventoryCheck1 = VFLFutures.supplyAsync(() -> checkInventory("LAPTOP-X1", 5), executor);
            var inventoryCheck2 = VFLFutures.supplyAsync(() -> checkInventory("MOUSE-PRO", 25), executor);
            var inventoryCheck3 = VFLFutures.supplyAsync(() -> checkInventory("KEYBOARD-MECH", 12), executor);

            // Wait for validations
            boolean order1Valid = orderValidation1.join();
            boolean order2Valid = orderValidation2.join();
            boolean order3Valid = orderValidation3.join();

            Log.Info("📋 Order validation results: ORD-001={}, ORD-002={}, ORD-003={}",
                    order1Valid, order2Valid, order3Valid);

            // Process payment for valid orders
            if (order1Valid) {
                var paymentProcessing = VFLFutures.runAsync(() -> processPayment("ORD-001", 299.99, "CREDIT_CARD"), executor);

                // Parallel shipping calculation
                var shippingCalc = VFLFutures.supplyAsync(() -> calculateShipping("ORD-001", "EXPRESS"), executor);

                paymentProcessing.join();
                double shippingCost = shippingCalc.join();

                // Final order processing
                finalizeOrder("ORD-001", 299.99 + shippingCost);
            }

            // Event-driven notifications
            var notificationEvent = Log.Publish("Order Processing Completed");

            // Multiple notification handlers
            sendEmailNotification(notificationEvent);
            sendSMSNotification(notificationEvent);
            updateAnalytics(notificationEvent);

            // Cleanup and reporting
            generateReport(inventoryCheck1.join(), inventoryCheck2.join(), inventoryCheck3.join());

            Log.Info("✅ Order processing pipeline completed successfully");
            executor.shutdown();
        });
    }

    @SubBlock(
            blockName = "User Authentication for {0}",
            startMessage = "🔐 Authenticating user {0}",
            endMessage = "✅ User {0} authenticated successfully"
    )
    private boolean authenticateUser(String email) {
        validateEmailFormat(email);
        checkUserCredentials(email);
        updateLastLoginTime(email);
        return true;
    }

    @SubBlock(
            blockName = "Email Validation",
            startMessage = "📧 Validating email format for {0}",
            endMessage = "Email format validation completed for {0}"
    )
    private void validateEmailFormat(String email) {
        Log.Info("Checking email pattern compliance");
        // Simulate validation delay
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Credential Check",
            startMessage = "🔍 Verifying credentials for {0}",
            endMessage = "Credentials verified for {0}"
    )
    private void checkUserCredentials(String email) {
        Log.Info("Querying user database");
        Log.Info("Validating password hash");
        // Simulate database query delay
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void updateLastLoginTime(String email) {
        Log.Info("📝 Updating last login timestamp for user");
        try { Thread.sleep(25); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Order Validation - {0}",
            startMessage = "📦 Starting validation for order {0} (${1})",
            endMessage = "Order {0} validation result: {r}"
    )
    private boolean validateOrder(String orderId, double amount) {
        checkOrderFormat(orderId);
        validateAmount(amount);
        verifyProductAvailability(orderId);

        // Simulate some orders failing validation
        return !orderId.equals("ORD-003"); // Simulate ORD-003 failing
    }

    @SubBlock
    private void checkOrderFormat(String orderId) {
        Log.Info("🔍 Validating order ID format");
        try { Thread.sleep(30); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void validateAmount(double amount) {
        Log.Info("💰 Validating order amount: ${}", amount);
        if (amount <= 0) {
            Log.Info("❌ Invalid amount detected");
        }
        try { Thread.sleep(20); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void verifyProductAvailability(String orderId) {
        Log.Info("📋 Checking product catalog for order");
        try { Thread.sleep(75); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Inventory Check - {0}",
            startMessage = "📊 Checking inventory for {0} (requested: {1})",
            endMessage = "Inventory check completed for {0}: {r} units available"
    )
    private int checkInventory(String productId, int requestedQuantity) {
        queryWarehouseSystem(productId);
        int available = calculateAvailableStock(productId, requestedQuantity);
        updateInventoryCache(productId, available);
        return available;
    }

    @SubBlock
    private void queryWarehouseSystem(String productId) {
        Log.Info("🏭 Querying warehouse management system");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
    }

    @SubBlock
    private int calculateAvailableStock(String productId, int requested) {
        Log.Info("🧮 Calculating available stock vs requested quantity");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
        // Simulate different stock levels
        return switch (productId) {
            case "LAPTOP-X1" -> 8;
            case "MOUSE-PRO" -> 30;
            case "KEYBOARD-MECH" -> 15;
            default -> 0;
        };
    }

    @SubBlock
    private void updateInventoryCache(String productId, int available) {
        Log.Info("💾 Updating inventory cache with latest counts");
        try { Thread.sleep(15); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Payment Processing - {0}",
            startMessage = "💳 Processing payment for {0}: ${1} via {2}",
            endMessage = "Payment processed successfully for {0}"
    )
    private void processPayment(String orderId, double amount, String method) {
        validatePaymentMethod(method);
        chargePaymentGateway(orderId, amount, method);
        recordPaymentTransaction(orderId, amount);
    }

    @SubBlock
    private void validatePaymentMethod(String method) {
        Log.Info("🔒 Validating payment method: {}", method);
        try { Thread.sleep(60); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void chargePaymentGateway(String orderId, double amount, String method) {
        Log.Info("🌐 Connecting to payment gateway");
        Log.Info("💸 Charging ${} via {}", amount, method);
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void recordPaymentTransaction(String orderId, double amount) {
        Log.Info("📝 Recording payment transaction in database");
        try { Thread.sleep(35); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Shipping Calculation - {0}",
            startMessage = "🚚 Calculating shipping for {0} with {1} delivery",
            endMessage = "Shipping cost calculated: ${r}"
    )
    private double calculateShipping(String orderId, String method) {
        double baseRate = getBaseShippingRate(method);
        double distance = calculateDistance(orderId);
        double finalCost = applyShippingDiscounts(baseRate, distance);
        return finalCost;
    }

    @SubBlock
    private double getBaseShippingRate(String method) {
        Log.Info("📋 Looking up base shipping rates");
        return "EXPRESS".equals(method) ? 15.99 : 5.99;
    }

    @SubBlock
    private double calculateDistance(String orderId) {
        Log.Info("🗺️ Calculating shipping distance");
        try { Thread.sleep(45); } catch (InterruptedException e) {}
        return 245.5; // Simulated distance in miles
    }

    @SubBlock
    private double applyShippingDiscounts(double baseRate, double distance) {
        Log.Info("🏷️ Applying shipping discounts and surcharges");
        double finalRate = baseRate + (distance > 200 ? 5.0 : 0.0);
        Log.Info("Final shipping cost after adjustments: ${}", finalRate);
        return finalRate;
    }

    @SubBlock(
            blockName = "Order Finalization - {0}",
            startMessage = "🎯 Finalizing order {0} with total ${1}",
            endMessage = "Order {0} finalized successfully"
    )
    private void finalizeOrder(String orderId, double totalAmount) {
        generateOrderConfirmation(orderId);
        updateOrderStatus(orderId, "CONFIRMED");
        scheduleFullfillment(orderId);
        Log.Info("💰 Final order total: ${}", totalAmount);
    }

    @SubBlock
    private void generateOrderConfirmation(String orderId) {
        Log.Info("📄 Generating order confirmation document");
        try { Thread.sleep(70); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void updateOrderStatus(String orderId, String status) {
        Log.Info("🔄 Updating order status to: {}", status);
        try { Thread.sleep(25); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void scheduleFullfillment(String orderId) {
        Log.Info("📅 Scheduling order for fulfillment");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
    }

    // Event listener methods
    void sendEmailNotification(EventPublisherBlock event) {
        VFLStarter.StartEventListener(event, "Email Notification Service", null, () -> {
            Log.Info("📧 Sending email notifications to customers");
            composeEmailContent();
            sendViaEmailProvider();
            trackEmailDelivery();
        });
    }

    void sendSMSNotification(EventPublisherBlock event) {
        VFLStarter.StartEventListener(event, "SMS Notification Service", null, () -> {
            Log.Info("📱 Sending SMS notifications");
            formatSMSMessage();
            sendViaSMSGateway();
        });
    }

    void updateAnalytics(EventPublisherBlock event) {
        VFLStarter.StartEventListener(event, "Analytics Service", null, () -> {
            Log.Info("📊 Updating analytics dashboard");
            recordOrderMetrics();
            updateCustomerSegmentation();
            generateRealtimeStats();
        });
    }

    @SubBlock
    private void composeEmailContent() {
        Log.Info("✍️ Composing personalized email content");
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void sendViaEmailProvider() {
        Log.Info("📮 Sending via email service provider");
        try { Thread.sleep(120); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void trackEmailDelivery() {
        Log.Info("📈 Setting up email delivery tracking");
        try { Thread.sleep(30); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void formatSMSMessage() {
        Log.Info("📝 Formatting SMS message within character limits");
        try { Thread.sleep(25); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void sendViaSMSGateway() {
        Log.Info("📲 Sending via SMS gateway");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void recordOrderMetrics() {
        Log.Info("📏 Recording order completion metrics");
        try { Thread.sleep(35); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void updateCustomerSegmentation() {
        Log.Info("👥 Updating customer segmentation data");
        try { Thread.sleep(60); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generateRealtimeStats() {
        Log.Info("⚡ Generating real-time statistics");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "System Report Generation",
            startMessage = "📊 Generating comprehensive system report",
            endMessage = "System report generated successfully"
    )
    private void generateReport(int laptop, int mouse, int keyboard) {
        Log.Info("📋 Compiling inventory levels: Laptops={}, Mice={}, Keyboards={}", laptop, mouse, keyboard);
        generateInventoryReport(laptop, mouse, keyboard);
        generatePerformanceReport();
        generateSummaryDashboard();
    }

    @SubBlock
    private void generateInventoryReport(int laptop, int mouse, int keyboard) {
        Log.Info("📦 Creating detailed inventory analysis");
        Log.Info("🔍 Identifying low-stock items");
        if (laptop < 10) Log.Info("⚠️ Low laptop inventory detected");
        if (mouse < 20) Log.Info("⚠️ Low mouse inventory detected");
        if (keyboard < 15) Log.Info("⚠️ Low keyboard inventory detected");
        try { Thread.sleep(90); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generatePerformanceReport() {
        Log.Info("⚡ Analyzing system performance metrics");
        Log.Info("📈 Processing time analysis completed");
        try { Thread.sleep(70); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generateSummaryDashboard() {
        Log.Info("🎯 Creating executive summary dashboard");
        try { Thread.sleep(55); } catch (InterruptedException e) {}
    }


}
