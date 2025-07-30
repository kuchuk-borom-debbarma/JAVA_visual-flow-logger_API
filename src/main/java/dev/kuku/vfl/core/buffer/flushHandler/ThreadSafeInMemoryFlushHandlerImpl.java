package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ThreadSafeInMemoryFlushHandlerImpl implements VFLFlushHandler {
    // Use thread-safe collections because if used in async buffer, it will cause race conditions when multiple flush happens
    public final ConcurrentLinkedQueue<Log> logs = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Block> blocks = new ConcurrentLinkedQueue<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        // addAll() on ConcurrentLinkedQueue is thread-safe
        this.logs.addAll(logs);
        System.out.println("Added " + logs.size() + " logs. Total logs now: " + this.logs.size());
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        this.blocks.addAll(blocks);
        System.out.println("Added " + blocks.size() + " blocks. Total blocks now: " + this.blocks.size());
        return true;
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        //TODO
        return false;
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, String> blockEnds) {
        //TODO
        return false;
    }

    public void cleanup() {
        logs.clear();
        blocks.clear();
    }

    /**
     * Generates flat JSON structure with blocks as separate top-level array elements
     * and logs containing references to other blocks
     *
     * @return JSON string representation of the flat structure
     */
    public String generateFlatJsonStructure() {
        try {
            ArrayNode rootArray = objectMapper.createArrayNode();

            // Convert concurrent collections to lists for processing
            List<Block> blockList = new ArrayList<>(blocks);
            List<Log> logList = new ArrayList<>(logs);

            // Create a map of block ID to block for quick lookup
            Map<String, Block> blockMap = blockList.stream()
                    .collect(Collectors.toMap(Block::getId, block -> block));

            // Group logs by block ID
            Map<String, List<Log>> logsByBlock = logList.stream()
                    .collect(Collectors.groupingBy(Log::getBlockId));

            // Create a set of all valid block IDs
            Set<String> validBlockIds = new HashSet<>(blockMap.keySet());

            // Process each block
            for (Block block : blockList) {
                ObjectNode blockNode = createFlatBlockNode(block, logsByBlock.get(block.getId()), blockMap);
                rootArray.add(blockNode);
            }

            // Handle logs with invalid block IDs
            List<Log> invalidLogs = logList.stream()
                    .filter(log -> !validBlockIds.contains(log.getBlockId()))
                    .collect(Collectors.toList());

            if (!invalidLogs.isEmpty()) {
                ObjectNode invalidBlockNode = createInvalidLogsBlock(invalidLogs, blockMap);
                rootArray.add(invalidBlockNode);
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootArray);

        } catch (Exception e) {
            throw new RuntimeException("Error generating flat JSON structure", e);
        }
    }

    /**
     * Generates nested JSON structure with referenced blocks embedded directly under
     * the logs that reference them, creating a true hierarchical structure
     *
     * @return JSON string representation of the nested structure
     */
    public String generateNestedJsonStructure() {
        try {
            ArrayNode rootArray = objectMapper.createArrayNode();

            // Convert concurrent collections to lists for processing
            List<Block> blockList = new ArrayList<>(blocks);
            List<Log> logList = new ArrayList<>(logs);

            // Create a map of block ID to block for quick lookup
            Map<String, Block> blockMap = blockList.stream()
                    .collect(Collectors.toMap(Block::getId, block -> block));

            // Group logs by block ID
            Map<String, List<Log>> logsByBlock = logList.stream()
                    .collect(Collectors.groupingBy(Log::getBlockId));

            // Create a set of all valid block IDs
            Set<String> validBlockIds = new HashSet<>(blockMap.keySet());

            // Track which blocks have been processed as nested blocks to avoid duplication
            Set<String> processedAsNestedBlocks = new HashSet<>();

            // Process each block
            for (Block block : blockList) {
                // Only add blocks that haven't been processed as nested blocks
                if (!processedAsNestedBlocks.contains(block.getId())) {
                    ObjectNode blockNode = createNestedBlockNode(block, logsByBlock.get(block.getId()),
                            blockMap, logsByBlock, processedAsNestedBlocks);
                    rootArray.add(blockNode);
                }
            }

            // Handle logs with invalid block IDs
            List<Log> invalidLogs = logList.stream()
                    .filter(log -> !validBlockIds.contains(log.getBlockId()))
                    .collect(Collectors.toList());

            if (!invalidLogs.isEmpty()) {
                ObjectNode invalidBlockNode = createInvalidLogsBlock(invalidLogs, blockMap);
                rootArray.add(invalidBlockNode);
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootArray);

        } catch (Exception e) {
            throw new RuntimeException("Error generating nested JSON structure", e);
        }
    }

    private ObjectNode createFlatBlockNode(Block block, List<Log> blockLogs, Map<String, Block> blockMap) {
        ObjectNode blockNode = objectMapper.createObjectNode();
        blockNode.put("id", block.getId());
        blockNode.put("name", block.getBlockName());

        if (blockLogs != null && !blockLogs.isEmpty()) {
            ArrayNode logsArray = objectMapper.createArrayNode();

            // Get root logs (logs without parent)
            List<Log> rootLogs = blockLogs.stream()
                    .filter(log -> log.getParentLogId() == null || log.getParentLogId().isEmpty())
                    .sorted(Comparator.comparingLong(Log::getTimestamp))
                    .toList();

            // Create a map for quick lookup of child logs
            Map<String, List<Log>> childLogMap = blockLogs.stream()
                    .filter(log -> log.getParentLogId() != null && !log.getParentLogId().isEmpty())
                    .collect(Collectors.groupingBy(Log::getParentLogId));

            for (Log rootLog : rootLogs) {
                ObjectNode logNode = createFlatLogNode(rootLog, childLogMap, blockMap);
                logsArray.add(logNode);
            }

            blockNode.set("logs", logsArray);
        } else {
            blockNode.set("logs", objectMapper.createArrayNode());
        }

        return blockNode;
    }

    private ObjectNode createNestedBlockNode(Block block, List<Log> blockLogs, Map<String, Block> blockMap,
                                             Map<String, List<Log>> allLogsByBlock, Set<String> processedAsNestedBlocks) {
        ObjectNode blockNode = objectMapper.createObjectNode();
        blockNode.put("id", block.getId());
        blockNode.put("name", block.getBlockName());

        if (blockLogs != null && !blockLogs.isEmpty()) {
            ArrayNode logsArray = objectMapper.createArrayNode();

            // Get root logs (logs without parent)
            List<Log> rootLogs = blockLogs.stream()
                    .filter(log -> log.getParentLogId() == null || log.getParentLogId().isEmpty())
                    .sorted(Comparator.comparingLong(Log::getTimestamp))
                    .toList();

            // Create a map for quick lookup of child logs
            Map<String, List<Log>> childLogMap = blockLogs.stream()
                    .filter(log -> log.getParentLogId() != null && !log.getParentLogId().isEmpty())
                    .collect(Collectors.groupingBy(Log::getParentLogId));

            for (Log rootLog : rootLogs) {
                ObjectNode logNode = createNestedLogNode(rootLog, childLogMap, blockMap,
                        allLogsByBlock, processedAsNestedBlocks);
                logsArray.add(logNode);
            }

            blockNode.set("logs", logsArray);
        } else {
            blockNode.set("logs", objectMapper.createArrayNode());
        }

        return blockNode;
    }

    private ObjectNode createFlatLogNode(Log log, Map<String, List<Log>> childLogMap, Map<String, Block> blockMap) {
        ObjectNode logNode = objectMapper.createObjectNode();
        logNode.put("id", log.getId());
        logNode.put("type", log.getLogType().toString());
        logNode.put("message", log.getMessage());

        // Check if this is a SubBlockStartLog and add referenced block info
        if (log instanceof SubBlockStartLog subBlockLog) {
            String referencedBlockId = subBlockLog.getReferencedBlockId();

            Block referencedBlock = blockMap.get(referencedBlockId);
            if (referencedBlock != null) {
                ObjectNode referencedBlockNode = objectMapper.createObjectNode();
                referencedBlockNode.put("id", referencedBlock.getId());
                referencedBlockNode.put("name", referencedBlock.getBlockName());
                logNode.set("referencedBlock", referencedBlockNode);
            }
        }

        // Add child logs
        List<Log> childLogs = childLogMap.get(log.getId());
        if (childLogs != null && !childLogs.isEmpty()) {
            ArrayNode childrenArray = objectMapper.createArrayNode();

            // Sort child logs by timestamp
            childLogs.sort(Comparator.comparingLong(Log::getTimestamp));

            for (Log childLog : childLogs) {
                ObjectNode childLogNode = createFlatLogNode(childLog, childLogMap, blockMap);
                childrenArray.add(childLogNode);
            }

            logNode.set("children", childrenArray);
        } else {
            logNode.set("children", objectMapper.createArrayNode());
        }

        return logNode;
    }

    private ObjectNode createNestedLogNode(Log log, Map<String, List<Log>> childLogMap, Map<String, Block> blockMap,
                                           Map<String, List<Log>> allLogsByBlock, Set<String> processedAsNestedBlocks) {
        ObjectNode logNode = objectMapper.createObjectNode();
        logNode.put("id", log.getId());
        logNode.put("type", log.getLogType().toString());
        logNode.put("message", log.getMessage());

        // Check if this is a SubBlockStartLog and embed the referenced block directly
        if (log instanceof SubBlockStartLog subBlockLog) {
            String referencedBlockId = subBlockLog.getReferencedBlockId();

            Block referencedBlock = blockMap.get(referencedBlockId);
            if (referencedBlock != null) {
                // Mark this block as processed so it won't appear at the top level
                processedAsNestedBlocks.add(referencedBlockId);

                // Create the full nested block structure
                ObjectNode referencedBlockNode = createNestedBlockNode(referencedBlock,
                        allLogsByBlock.get(referencedBlockId),
                        blockMap, allLogsByBlock, processedAsNestedBlocks);
                logNode.set("referencedBlock", referencedBlockNode);
            }
        }

        // Add child logs
        List<Log> childLogs = childLogMap.get(log.getId());
        if (childLogs != null && !childLogs.isEmpty()) {
            ArrayNode childrenArray = objectMapper.createArrayNode();

            // Sort child logs by timestamp
            childLogs.sort(Comparator.comparingLong(Log::getTimestamp));

            for (Log childLog : childLogs) {
                ObjectNode childLogNode = createNestedLogNode(childLog, childLogMap, blockMap,
                        allLogsByBlock, processedAsNestedBlocks);
                childrenArray.add(childLogNode);
            }

            logNode.set("children", childrenArray);
        } else {
            logNode.set("children", objectMapper.createArrayNode());
        }

        return logNode;
    }

    private ObjectNode createInvalidLogsBlock(List<Log> invalidLogs, Map<String, Block> blockMap) {
        ObjectNode invalidBlockNode = objectMapper.createObjectNode();
        invalidBlockNode.put("id", "invalid-logs-block");
        invalidBlockNode.put("name", "Invalid Logs");

        ArrayNode logsArray = objectMapper.createArrayNode();

        // Group invalid logs by block ID for better organization
        Map<String, List<Log>> invalidLogsByBlockId = invalidLogs.stream()
                .collect(Collectors.groupingBy(Log::getBlockId));

        // Create a map for child log lookup among invalid logs
        Map<String, List<Log>> childLogMap = invalidLogs.stream()
                .filter(log -> log.getParentLogId() != null && !log.getParentLogId().isEmpty())
                .collect(Collectors.groupingBy(Log::getParentLogId));

        for (Map.Entry<String, List<Log>> entry : invalidLogsByBlockId.entrySet()) {
            String invalidBlockId = entry.getKey();
            List<Log> logsForInvalidBlock = entry.getValue();

            // Get root logs for this invalid block ID
            List<Log> rootLogs = logsForInvalidBlock.stream()
                    .filter(log -> log.getParentLogId() == null || log.getParentLogId().isEmpty())
                    .sorted(Comparator.comparingLong(Log::getTimestamp))
                    .toList();

            for (Log rootLog : rootLogs) {
                ObjectNode logNode = createFlatLogNode(rootLog, childLogMap, blockMap);
                // Add additional info about the invalid block ID
                logNode.put("originalBlockId", invalidBlockId);
                logsArray.add(logNode);
            }
        }

        invalidBlockNode.set("logs", logsArray);
        return invalidBlockNode;
    }

    /**
     * Alternative method that returns the flat structure as a Map for programmatic access
     *
     * @return Map representation of the flat structure
     */
    public Map<String, Object> generateFlatStructureAsMap() {
        try {
            String jsonString = generateFlatJsonStructure();
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting flat JSON to Map", e);
        }
    }

    /**
     * Alternative method that returns the nested structure as a Map for programmatic access
     *
     * @return Map representation of the nested structure
     */
    public Map<String, Object> generateNestedStructureAsMap() {
        try {
            String jsonString = generateNestedJsonStructure();
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting nested JSON to Map", e);
        }
    }
}