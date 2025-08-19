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

    // Original simple methods (keeping for backward compatibility)
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

    // Enhanced Linear Method - Document Processing Pipeline
    @SubBlock(
            blockName = "Document Processing Pipeline",
            startMessage = "📄 Starting linear document processing workflow",
            endMessage = "✅ Document processing completed successfully"
    )
    public void linear() {
        VFLStarter.StartRootBlock("Linear Document Processing Pipeline", () -> {
            Log.Info("🚀 Initializing document processing system");

            // Step 1: Load and validate document
            String documentPath = "/uploads/report_2025.pdf";
            DocumentMetadata metadata = Log.InfoFn(
                    () -> loadDocument(documentPath),
                    "Loading document from: {} | Result: {}",
                    documentPath
            );

            // Step 2: Extract and process content
            String rawContent = extractContent(metadata);
            Log.Info("📊 Raw content extracted: {} characters", rawContent.length());

            // Step 3: Multi-level text processing
            ProcessedContent processedContent = processTextContent(rawContent);

            // Step 4: Generate outputs
            ReportSummary summary = generateSummary(processedContent);

            // Step 5: Save results
            String outputPath = saveResults(summary, metadata);

            Log.Info("🎯 Pipeline completed. Output saved to: {}", outputPath);
            Log.Info("📈 Processing stats - Words: {}, Pages: {}, Processing time: {}ms",
                    summary.wordCount, metadata.pageCount, metadata.processingTimeMs);
        });
    }

    @SubBlock(
            blockName = "Document Loading - {0}",
            startMessage = "📂 Loading document from {0}",
            endMessage = "Document loaded: {r.fileName} ({r.sizeKb}KB, {r.pageCount} pages)"
    )
    private DocumentMetadata loadDocument(String path) {
        validateFilePath(path);
        FileInfo fileInfo = readFileProperties(path);
        DocumentMetadata metadata = parseDocumentMetadata(fileInfo);
        return metadata;
    }

    @SubBlock(
            blockName = "File Path Validation",
            startMessage = "🔍 Validating file path structure",
            endMessage = "File path validation completed"
    )
    private void validateFilePath(String path) {
        Log.Info("Checking path format and accessibility");
        // Simulate validation
        try { Thread.sleep(20); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "File Properties Reader",
            startMessage = "📋 Reading file system properties",
            endMessage = "File properties extracted successfully"
    )
    private FileInfo readFileProperties(String path) {
        Log.Info("Accessing file system metadata");
        Log.Info("Calculating file size and permissions");
        // Simulate file I/O
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        return new FileInfo("report_2025.pdf", 2048, "application/pdf");
    }

    @SubBlock(
            blockName = "Document Metadata Parser",
            startMessage = "🔬 Parsing document internal metadata",
            endMessage = "Metadata parsing completed - {r.pageCount} pages identified"
    )
    private DocumentMetadata parseDocumentMetadata(FileInfo fileInfo) {
        Log.Info("Extracting document properties and structure");
        extractAuthorInfo(fileInfo);
        int pageCount = countPages(fileInfo);
        String[] keywords = extractKeywords(fileInfo);

        return new DocumentMetadata(fileInfo.fileName, fileInfo.sizeKb, pageCount,
                System.currentTimeMillis(), keywords);
    }

    @SubBlock
    private void extractAuthorInfo(FileInfo fileInfo) {
        Log.Info("🏷️ Extracting author and creation metadata");
        try { Thread.sleep(30); } catch (InterruptedException e) {}
    }

    @SubBlock
    private int countPages(FileInfo fileInfo) {
        Log.Info("📄 Counting document pages");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
        return 24; // Simulated page count
    }

    @SubBlock
    private String[] extractKeywords(FileInfo fileInfo) {
        Log.Info("🔖 Extracting document keywords and tags");
        try { Thread.sleep(35); } catch (InterruptedException e) {}
        return new String[]{"analysis", "report", "quarterly", "metrics"};
    }

    @SubBlock(
            blockName = "Content Extraction",
            startMessage = "📖 Extracting textual content from document",
            endMessage = "Content extraction completed: {r} characters extracted"
    )
    private String extractContent(DocumentMetadata metadata) {
        String rawText = performOCRExtraction(metadata);
        String cleanedText = cleanExtractedText(rawText);
        return cleanedText;
    }

    @SubBlock
    private String performOCRExtraction(DocumentMetadata metadata) {
        Log.Info("🔍 Running OCR engine on document pages");
        Log.Info("Processing {} pages with text recognition", metadata.pageCount);
        try { Thread.sleep(120); } catch (InterruptedException e) {}
        return "This is extracted content from the document..."; // Simulated content
    }

    @SubBlock
    private String cleanExtractedText(String rawText) {
        Log.Info("🧹 Cleaning and formatting extracted text");
        Log.Info("Removing artifacts, normalizing whitespace");
        try { Thread.sleep(60); } catch (InterruptedException e) {}
        return rawText.trim();
    }

    @SubBlock(
            blockName = "Text Content Processing",
            startMessage = "⚙️ Processing extracted content through NLP pipeline",
            endMessage = "Text processing completed: {r.wordCount} words, {r.sentenceCount} sentences"
    )
    private ProcessedContent processTextContent(String content) {
        TokenizedContent tokens = tokenizeText(content);
        AnalyzedContent analysis = performLanguageAnalysis(tokens);
        ProcessedContent processed = enrichContent(analysis);
        return processed;
    }

    @SubBlock(
            blockName = "Text Tokenization",
            startMessage = "🔤 Tokenizing text into words and sentences",
            endMessage = "Tokenization completed: {r.wordTokens} words, {r.sentenceTokens} sentences"
    )
    private TokenizedContent tokenizeText(String content) {
        Log.Info("Breaking text into linguistic units");
        Log.Info("Identifying sentence boundaries and word tokens");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
        return new TokenizedContent(245, 18);
    }

    @SubBlock
    private AnalyzedContent performLanguageAnalysis(TokenizedContent tokens) {
        Log.Info("📊 Performing linguistic analysis");
        detectLanguage(tokens);
        analyzeSentiment(tokens);
        extractEntities(tokens);
        return new AnalyzedContent("positive", new String[]{"Q1", "revenue", "growth"});
    }

    @SubBlock
    private void detectLanguage(TokenizedContent tokens) {
        Log.Info("🌐 Detecting document language");
        try { Thread.sleep(25); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void analyzeSentiment(TokenizedContent tokens) {
        Log.Info("😊 Analyzing content sentiment");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void extractEntities(TokenizedContent tokens) {
        Log.Info("🏢 Extracting named entities and key phrases");
        try { Thread.sleep(55); } catch (InterruptedException e) {}
    }

    @SubBlock
    private ProcessedContent enrichContent(AnalyzedContent analysis) {
        Log.Info("✨ Enriching content with analysis results");
        try { Thread.sleep(45); } catch (InterruptedException e) {}
        return new ProcessedContent(245, 18, analysis.sentiment, analysis.entities);
    }

    @SubBlock(
            blockName = "Report Summary Generation",
            startMessage = "📋 Generating executive summary",
            endMessage = "Summary generated: {r.wordCount} words, {r.keyPoints} key points"
    )
    private ReportSummary generateSummary(ProcessedContent content) {
        String[] keyPoints = identifyKeyPoints(content);
        String summaryText = createSummaryText(keyPoints, content);
        ReportSummary summary = compileSummary(summaryText, keyPoints, content);
        return summary;
    }

    @SubBlock
    private String[] identifyKeyPoints(ProcessedContent content) {
        Log.Info("🎯 Identifying key discussion points");
        try { Thread.sleep(70); } catch (InterruptedException e) {}
        return new String[]{"Revenue increased 15%", "Customer satisfaction improved", "New market expansion"};
    }

    @SubBlock
    private String createSummaryText(String[] keyPoints, ProcessedContent content) {
        Log.Info("✍️ Composing summary narrative");
        try { Thread.sleep(90); } catch (InterruptedException e) {}
        return "Executive Summary: Strong quarterly performance...";
    }

    @SubBlock
    private ReportSummary compileSummary(String summaryText, String[] keyPoints, ProcessedContent content) {
        Log.Info("📊 Compiling final summary report");
        try { Thread.sleep(35); } catch (InterruptedException e) {}
        return new ReportSummary(summaryText, 85, keyPoints.length, content.wordCount);
    }

    @SubBlock(
            blockName = "Results Persistence",
            startMessage = "💾 Saving processing results to storage",
            endMessage = "Results saved to: {r}"
    )
    private String saveResults(ReportSummary summary, DocumentMetadata metadata) {
        String outputPath = generateOutputPath(metadata);
        saveToDatabase(summary, metadata);
        saveToFileSystem(summary, outputPath);
        return outputPath;
    }

    @SubBlock
    private String generateOutputPath(DocumentMetadata metadata) {
        Log.Info("🗂️ Generating output file path");
        return "/output/processed_" + metadata.fileName.replace(".pdf", "_summary.json");
    }

    @SubBlock
    private void saveToDatabase(ReportSummary summary, DocumentMetadata metadata) {
        Log.Info("🗄️ Persisting results to database");
        try { Thread.sleep(60); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void saveToFileSystem(ReportSummary summary, String path) {
        Log.Info("📁 Writing results to file system");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
    }

    // Enhanced Async Method - Parallel Data Processing Pipeline
    public void async() {
        VFLStarter.StartRootBlock("Parallel Data Processing Pipeline", () -> {
            Log.Info("🚀 Initializing parallel processing system with multiple data sources");

            var executor = Executors.newFixedThreadPool(6);

            Log.Info("📊 Starting parallel data ingestion from multiple sources");

            // Parallel data ingestion tasks (independent)
            var salesDataTask = VFLFutures.supplyAsync(() -> {
                return loadSalesData("Q1_2025_sales.csv");
            }, executor);

            var customerDataTask = VFLFutures.supplyAsync(() -> {
                return loadCustomerData("customer_profiles.json");
            }, executor);

            var inventoryDataTask = VFLFutures.supplyAsync(() -> {
                return loadInventoryData("inventory_snapshot.xml");
            }, executor);

            Log.Info("⚙️ Starting parallel data preprocessing operations");

            // Data preprocessing (depends on ingestion)
            var cleanedSalesTask = salesDataTask.thenApplyAsync(this::cleanSalesData, executor);

            var enrichedCustomerTask = customerDataTask.thenApplyAsync(this::enrichCustomerData, executor);

            // Parallel computation tasks (I/O intensive)
            var reportGenerationTask = VFLFutures.runAsync(() -> {
                generateMonthlyReports();
            }, executor);

            var cacheWarmupTask = VFLFutures.runAsync(() -> {
                warmupApplicationCache();
            }, executor);

            Log.Info("🔄 Coordinating dependent processing tasks");

            // Wait for critical data processing
            SalesData cleanedSales = cleanedSalesTask.join();
            CustomerData enrichedCustomers = enrichedCustomerTask.join();
            InventoryData inventory = inventoryDataTask.join();

            Log.Info("✅ Core data loading completed - Sales: {} records, Customers: {} records, Inventory: {} items",
                    cleanedSales.recordCount, enrichedCustomers.profileCount, inventory.itemCount);

            // Dependent analysis tasks (require multiple data sources)
            var crossAnalysisTask = VFLFutures.supplyAsync(() -> {
                return performCrossDataAnalysis(cleanedSales, enrichedCustomers, inventory);
            }, executor);

            var trendAnalysisTask = VFLFutures.supplyAsync(() -> {
                return analyzeTrends(cleanedSales, enrichedCustomers);
            }, executor);

            Log.Info("📈 Performing advanced analytics");

            // Final results coordination
            AnalysisResults crossAnalysis = crossAnalysisTask.join();
            TrendAnalysis trends = trendAnalysisTask.join();

            // Wait for background tasks to complete
            reportGenerationTask.join();
            cacheWarmupTask.join();

            // Final consolidation
            var consolidationTask = VFLFutures.supplyAsync(() -> {
                return consolidateResults(crossAnalysis, trends);
            }, executor);

            ConsolidatedReport finalReport = consolidationTask.join();

            Log.Info("🎯 Parallel processing pipeline completed successfully");
            Log.Info("📊 Final metrics - Analysis points: {}, Trend indicators: {}, Processing threads used: {}",
                    finalReport.analysisPoints, finalReport.trendIndicators, 6);

            executor.shutdown();
        });
    }

    @SubBlock(
            blockName = "Sales Data Loading",
            startMessage = "📈 Loading sales data from {0}",
            endMessage = "Sales data loaded: {r.recordCount} records from {r.timeRange}"
    )
    private SalesData loadSalesData(String fileName) {
        Log.Info("🔍 Accessing sales database connection");
        validateDataSource(fileName);

        Log.InfoFn(() -> {
            try { Thread.sleep(150); } catch (InterruptedException e) {}
            return "Connection established";
        }, "Database connection status: {}");

        SalesData data = extractSalesRecords(fileName);
        Log.Info("💰 Sales data extraction completed");
        return data;
    }

    @SubBlock(
            blockName = "Customer Data Loading",
            startMessage = "👥 Loading customer profiles from {0}",
            endMessage = "Customer data loaded: {r.profileCount} profiles, {r.segmentCount} segments"
    )
    private CustomerData loadCustomerData(String fileName) {
        Log.Info("🔗 Establishing customer database connection");
        parseCustomerProfiles(fileName);

        CustomerData data = Log.InfoFn(() -> {
            try { Thread.sleep(120); } catch (InterruptedException e) {}
            return new CustomerData(1250, 5, "2025-Q1");
        }, "Customer data processing result: {}");

        return data;
    }

    @SubBlock(
            blockName = "Inventory Data Loading",
            startMessage = "📦 Loading inventory snapshot from {0}",
            endMessage = "Inventory loaded: {r.itemCount} items, {r.warehouseCount} warehouses"
    )
    private InventoryData loadInventoryData(String fileName) {
        Log.Info("🏭 Connecting to inventory management system");
        InventoryData data = processInventoryXML(fileName);
        validateInventoryData(data);
        return data;
    }

    @SubBlock
    private void validateDataSource(String fileName) {
        Log.Info("✅ Validating data source integrity: {}", fileName);
        try { Thread.sleep(30); } catch (InterruptedException e) {}
    }

    @SubBlock
    private SalesData extractSalesRecords(String fileName) {
        Log.Info("📊 Extracting sales records and transactions");
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        return new SalesData(3420, "Q1 2025");
    }

    @SubBlock
    private void parseCustomerProfiles(String fileName) {
        Log.Info("🔬 Parsing customer profile data structure");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
    }

    @SubBlock
    private InventoryData processInventoryXML(String fileName) {
        Log.Info("📋 Processing XML inventory structure");
        try { Thread.sleep(90); } catch (InterruptedException e) {}
        return new InventoryData(8750, 12);
    }

    @SubBlock
    private void validateInventoryData(InventoryData data) {
        Log.Info("🔍 Validating inventory data consistency");
        try { Thread.sleep(40); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Sales Data Cleaning",
            startMessage = "🧹 Cleaning and standardizing sales data",
            endMessage = "Sales data cleaning completed: {r.recordCount} clean records"
    )
    private SalesData cleanSalesData(SalesData rawData) {
        Log.Info("🔧 Removing duplicate transactions");
        removeDuplicates(rawData);

        Log.Info("📝 Standardizing data formats");
        standardizeFormats(rawData);

        Log.Info("🎯 Applying data quality rules");
        applyQualityRules(rawData);

        return new SalesData(rawData.recordCount - 45, rawData.timeRange); // Removed some duplicates
    }

    @SubBlock
    private void removeDuplicates(SalesData data) {
        Log.Info("🔍 Identifying and removing duplicate entries");
        try { Thread.sleep(60); } catch (InterruptedException ignored) {}
    }

    @SubBlock
    private void standardizeFormats(SalesData data) {
        Log.Info("📐 Standardizing date, currency, and field formats");
        try { Thread.sleep(45); } catch (InterruptedException ignored) {}
    }

    @SubBlock
    private void applyQualityRules(SalesData data) {
        Log.Info("✅ Applying business logic and validation rules");
        try { Thread.sleep(35); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Customer Data Enrichment",
            startMessage = "✨ Enriching customer profiles with additional data",
            endMessage = "Customer enrichment completed: {r.profileCount} enhanced profiles"
    )
    private CustomerData enrichCustomerData(CustomerData rawData) {
        addDemographicData(rawData);
        calculateLifetimeValue(rawData);
        updateSegmentation(rawData);
        return new CustomerData(rawData.profileCount, rawData.segmentCount + 2, rawData.period);
    }

    @SubBlock
    private void addDemographicData(CustomerData data) {
        Log.Info("👤 Adding demographic information to profiles");
        try { Thread.sleep(70); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void calculateLifetimeValue(CustomerData data) {
        Log.Info("💎 Calculating customer lifetime value metrics");
        try { Thread.sleep(85); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void updateSegmentation(CustomerData data) {
        Log.Info("🎯 Updating customer segmentation categories");
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Monthly Report Generation",
            startMessage = "📊 Generating automated monthly reports",
            endMessage = "Monthly reports generated successfully"
    )
    private void generateMonthlyReports() {
        Log.Info("📈 Creating sales performance reports");
        generateSalesReport();

        Log.Info("📋 Creating operational summaries");
        generateOperationalReport();

        Log.Info("💼 Creating executive dashboards");
        generateExecutiveDashboard();
    }

    @SubBlock
    private void generateSalesReport() {
        Log.Info("💰 Compiling sales metrics and KPIs");
        try { Thread.sleep(110); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generateOperationalReport() {
        Log.Info("⚙️ Analyzing operational efficiency metrics");
        try { Thread.sleep(95); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generateExecutiveDashboard() {
        Log.Info("🎯 Creating high-level executive insights");
        try { Thread.sleep(75); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Application Cache Warmup",
            startMessage = "🔥 Warming up application caches",
            endMessage = "Cache warmup completed successfully"
    )
    private void warmupApplicationCache() {
        Log.Info("💾 Pre-loading frequently accessed data");
        preloadReferenceData();

        Log.Info("🔍 Warming up search indices");
        warmupSearchCache();

        Log.Info("📊 Pre-calculating common aggregations");
        precalculateMetrics();
    }

    @SubBlock
    private void preloadReferenceData() {
        Log.Info("📚 Loading reference tables and lookup data");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void warmupSearchCache() {
        Log.Info("🔎 Pre-warming search and filter caches");
        try { Thread.sleep(65); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void precalculateMetrics() {
        Log.Info("🧮 Pre-calculating dashboard metrics");
        try { Thread.sleep(90); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Cross-Data Analysis",
            startMessage = "🔬 Performing cross-dataset analysis",
            endMessage = "Cross-analysis completed: {r.correlationCount} correlations found"
    )
    private AnalysisResults performCrossDataAnalysis(SalesData sales, CustomerData customers, InventoryData inventory) {
        int correlations = analyzeSalesCustomerCorrelations(sales, customers);
        analyzeInventoryTurnover(sales, inventory);
        identifyCustomerPurchasePatterns(customers, sales);

        return new AnalysisResults(correlations, "cross-dataset");
    }

    @SubBlock
    private int analyzeSalesCustomerCorrelations(SalesData sales, CustomerData customers) {
        Log.Info("🔗 Analyzing sales-customer relationship patterns");
        try { Thread.sleep(120); } catch (InterruptedException e) {}
        return 23; // Simulated correlation count
    }

    @SubBlock
    private void analyzeInventoryTurnover(SalesData sales, InventoryData inventory) {
        Log.Info("📦 Calculating inventory turnover rates");
        try { Thread.sleep(95); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void identifyCustomerPurchasePatterns(CustomerData customers, SalesData sales) {
        Log.Info("🛒 Identifying customer purchase behavior patterns");
        try { Thread.sleep(110); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Trend Analysis",
            startMessage = "📈 Analyzing data trends and patterns",
            endMessage = "Trend analysis completed: {r.trendCount} trends identified"
    )
    private TrendAnalysis analyzeTrends(SalesData sales, CustomerData customers) {
        identifyGrowthTrends(sales);
        analyzeSeasonalPatterns(sales);
        evaluateCustomerBehaviorTrends(customers);

        return new TrendAnalysis(15, "quarterly-trends");
    }

    @SubBlock
    private void identifyGrowthTrends(SalesData sales) {
        Log.Info("📊 Identifying revenue and growth trends");
        try { Thread.sleep(85); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void analyzeSeasonalPatterns(SalesData sales) {
        Log.Info("🗓️ Analyzing seasonal sales patterns");
        try { Thread.sleep(70); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void evaluateCustomerBehaviorTrends(CustomerData customers) {
        Log.Info("👥 Evaluating customer behavior evolution");
        try { Thread.sleep(90); } catch (InterruptedException e) {}
    }

    @SubBlock(
            blockName = "Results Consolidation",
            startMessage = "🔄 Consolidating analysis results",
            endMessage = "Consolidation completed: {r.analysisPoints} data points, {r.trendIndicators} indicators"
    )
    private ConsolidatedReport consolidateResults(AnalysisResults analysis, TrendAnalysis trends) {
        Log.Info("📋 Merging analysis results and trend data");
        mergeDatasets(analysis, trends);

        Log.Info("🎯 Creating unified insights dashboard");
        generateUnifiedInsights(analysis, trends);

        return new ConsolidatedReport(analysis.correlationCount + trends.trendCount,
                trends.trendCount, "Q1-2025-consolidated");
    }

    @SubBlock
    private void mergeDatasets(AnalysisResults analysis, TrendAnalysis trends) {
        Log.Info("🔗 Merging multi-source analysis results");
        try { Thread.sleep(60); } catch (InterruptedException e) {}
    }

    @SubBlock
    private void generateUnifiedInsights(AnalysisResults analysis, TrendAnalysis trends) {
        Log.Info("💡 Generating actionable business insights");
        try { Thread.sleep(80); } catch (InterruptedException e) {}
    }

    // Original Event Publisher Test (keeping for backward compatibility)
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

    // Original Complex Real World Simulation (keeping for backward compatibility)
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

// Data classes for the enhanced tests
class DocumentMetadata {
    String fileName;
    int sizeKb;
    int pageCount;
    long processingTimeMs;
    String[] keywords;

    DocumentMetadata(String fileName, int sizeKb, int pageCount, long processingTimeMs, String[] keywords) {
        this.fileName = fileName;
        this.sizeKb = sizeKb;
        this.pageCount = pageCount;
        this.processingTimeMs = processingTimeMs;
        this.keywords = keywords;
    }
}

class FileInfo {
    String fileName;
    int sizeKb;
    String mimeType;

    FileInfo(String fileName, int sizeKb, String mimeType) {
        this.fileName = fileName;
        this.sizeKb = sizeKb;
        this.mimeType = mimeType;
    }
}

class TokenizedContent {
    int wordTokens;
    int sentenceTokens;

    TokenizedContent(int wordTokens, int sentenceTokens) {
        this.wordTokens = wordTokens;
        this.sentenceTokens = sentenceTokens;
    }
}

class AnalyzedContent {
    String sentiment;
    String[] entities;

    AnalyzedContent(String sentiment, String[] entities) {
        this.sentiment = sentiment;
        this.entities = entities;
    }
}

class ProcessedContent {
    int wordCount;
    int sentenceCount;
    String sentiment;
    String[] entities;

    ProcessedContent(int wordCount, int sentenceCount, String sentiment, String[] entities) {
        this.wordCount = wordCount;
        this.sentenceCount = sentenceCount;
        this.sentiment = sentiment;
        this.entities = entities;
    }
}

class ReportSummary {
    String summaryText;
    int wordCount;
    int keyPoints;
    int sourceWordCount;

    ReportSummary(String summaryText, int wordCount, int keyPoints, int sourceWordCount) {
        this.summaryText = summaryText;
        this.wordCount = wordCount;
        this.keyPoints = keyPoints;
        this.sourceWordCount = sourceWordCount;
    }
}

class SalesData {
    int recordCount;
    String timeRange;

    SalesData(int recordCount, String timeRange) {
        this.recordCount = recordCount;
        this.timeRange = timeRange;
    }
}

class CustomerData {
    int profileCount;
    int segmentCount;
    String period;

    CustomerData(int profileCount, int segmentCount, String period) {
        this.profileCount = profileCount;
        this.segmentCount = segmentCount;
        this.period = period;
    }
}

class InventoryData {
    int itemCount;
    int warehouseCount;

    InventoryData(int itemCount, int warehouseCount) {
        this.itemCount = itemCount;
        this.warehouseCount = warehouseCount;
    }
}

class AnalysisResults {
    int correlationCount;
    String analysisType;

    AnalysisResults(int correlationCount, String analysisType) {
        this.correlationCount = correlationCount;
        this.analysisType = analysisType;
    }
}

class TrendAnalysis {
    int trendCount;
    String periodType;

    TrendAnalysis(int trendCount, String periodType) {
        this.trendCount = trendCount;
        this.periodType = periodType;
    }
}

class ConsolidatedReport {
    int analysisPoints;
    int trendIndicators;
    String reportId;

    ConsolidatedReport(int analysisPoints, int trendIndicators, String reportId) {
        this.analysisPoints = analysisPoints;
        this.trendIndicators = trendIndicators;
        this.reportId = reportId;
    }
}