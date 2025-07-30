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
     * Generates nested JSON structure with blocks containing their logs in hierarchical format
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

            // Process each block
            for (Block block : blockList) {
                ObjectNode blockNode = createBlockNode(block, logsByBlock.get(block.getId()), blockMap);
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
            throw new RuntimeException("Error generating nested JSON structure", e);
        }
    }

    private ObjectNode createBlockNode(Block block, List<Log> blockLogs, Map<String, Block> blockMap) {
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
                ObjectNode logNode = createLogNode(rootLog, childLogMap, blockMap);
                logsArray.add(logNode);
            }

            blockNode.set("logs", logsArray);
        } else {
            blockNode.set("logs", objectMapper.createArrayNode());
        }

        return blockNode;
    }

    private ObjectNode createLogNode(Log log, Map<String, List<Log>> childLogMap, Map<String, Block> blockMap) {
        ObjectNode logNode = objectMapper.createObjectNode();
        logNode.put("id", log.getId());
        logNode.put("type", log.getLogType().toString());
        logNode.put("message", log.getMessage());

        // Check if this is a SubBlockStartLog and add referenced block
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
                ObjectNode childLogNode = createLogNode(childLog, childLogMap, blockMap);
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
                ObjectNode logNode = createLogNode(rootLog, childLogMap, blockMap);
                // Add additional info about the invalid block ID
                logNode.put("originalBlockId", invalidBlockId);
                logsArray.add(logNode);
            }
        }

        invalidBlockNode.set("logs", logsArray);
        return invalidBlockNode;
    }

    /**
     * Alternative method that returns the structure as a Map for programmatic access
     *
     * @return Map representation of the nested structure
     */
    public Map<String, Object> generateNestedStructureAsMap() {
        try {
            String jsonString = generateNestedJsonStructure();
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to Map", e);
        }
    }
}