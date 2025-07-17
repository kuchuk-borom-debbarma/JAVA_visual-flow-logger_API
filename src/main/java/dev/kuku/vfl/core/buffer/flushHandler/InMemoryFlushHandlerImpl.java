package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryFlushHandlerImpl implements VFLFlushHandler {
    public List<LogData> logs = new ArrayList<>();
    public List<BlockData> blocks = new ArrayList<>();

    @Override
    public boolean pushLogsToServer(List<LogData> logs) {
        this.logs.addAll(logs);
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<BlockData> blocks) {
        this.blocks.addAll(blocks);
        return true;
    }

    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode resultArray = mapper.createArrayNode();

            // Create set of valid block IDs for quick lookup
            Set<String> validBlockIds = blocks.stream()
                    .map(BlockData::getId)
                    .collect(Collectors.toSet());

            // Separate valid and invalid logs
            Map<String, List<LogData>> validLogsByBlockId = new HashMap<>();
            List<LogData> invalidLogs = new ArrayList<>();

            for (LogData log : logs) {
                if (log.getBlockId() != null && validBlockIds.contains(log.getBlockId())) {
                    validLogsByBlockId.computeIfAbsent(log.getBlockId(), k -> new ArrayList<>()).add(log);
                } else {
                    invalidLogs.add(log);
                }
            }

            // Process each valid block
            for (BlockData block : blocks) {
                ObjectNode blockNode = mapper.createObjectNode();
                blockNode.put("blockId", block.getId());
                blockNode.put("blockName", block.getBlockName());
                blockNode.put("parentBlockId", block.getParentBlockId());

                // Get logs for this block
                List<LogData> blockLogs = validLogsByBlockId.getOrDefault(block.getId(), new ArrayList<>());

                // Sort logs hierarchically based on parentLogId
                List<LogData> sortedLogs = sortLogsHierarchically(blockLogs);

                // Convert logs to JSON array
                ArrayNode logsArray = mapper.createArrayNode();
                for (LogData log : sortedLogs) {
                    ObjectNode logNode = createLogNode(mapper, log);
                    logsArray.add(logNode);
                }

                blockNode.set("logs", logsArray);
                resultArray.add(blockNode);
            }

            // Add invalid logs section if there are any
            if (!invalidLogs.isEmpty()) {
                ObjectNode invalidSection = mapper.createObjectNode();
                invalidSection.put("blockId", "INVALID_LOGS");
                invalidSection.put("blockName", "Invalid Logs");
                invalidSection.putNull("parentBlockId");

                // Sort invalid logs hierarchically too
                List<LogData> sortedInvalidLogs = sortLogsHierarchically(invalidLogs);

                ArrayNode invalidLogsArray = mapper.createArrayNode();
                for (LogData log : sortedInvalidLogs) {
                    ObjectNode logNode = createLogNode(mapper, log);
                    invalidLogsArray.add(logNode);
                }

                invalidSection.set("logs", invalidLogsArray);
                resultArray.add(invalidSection);
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultArray);

        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON string", e);
        }
    }

    public String toJsonNested() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode resultArray = mapper.createArrayNode();

            // Create maps for quick lookup
            Map<String, BlockData> blockMap = blocks.stream()
                    .collect(Collectors.toMap(BlockData::getId, block -> block));

            Set<String> validBlockIds = blocks.stream()
                    .map(BlockData::getId)
                    .collect(Collectors.toSet());

            // Separate valid and invalid logs
            Map<String, List<LogData>> validLogsByBlockId = new HashMap<>();
            List<LogData> invalidLogs = new ArrayList<>();

            for (LogData log : logs) {
                if (log.getBlockId() != null && validBlockIds.contains(log.getBlockId())) {
                    validLogsByBlockId.computeIfAbsent(log.getBlockId(), k -> new ArrayList<>()).add(log);
                } else {
                    invalidLogs.add(log);
                }
            }

            // Find blocks that are referenced by SUB_BLOCK_START logs
            Set<String> referencedBlockIds = logs.stream()
                    .filter(log -> log.getLogType() != null && "SUB_BLOCK_START".equals(log.getLogType().toString()))
                    .map(LogData::getReferencedBlock)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Process only root blocks (blocks that are not referenced by SUB_BLOCK_START logs)
            for (BlockData block : blocks) {
                if (!referencedBlockIds.contains(block.getId())) {
                    ObjectNode blockNode = createNestedBlockNode(mapper, block, validLogsByBlockId, blockMap);
                    resultArray.add(blockNode);
                }
            }

            // Add invalid logs section if there are any
            if (!invalidLogs.isEmpty()) {
                ObjectNode invalidSection = mapper.createObjectNode();
                invalidSection.put("blockId", "INVALID_LOGS");
                invalidSection.put("blockName", "Invalid Logs");
                invalidSection.putNull("parentBlockId");

                // Sort invalid logs hierarchically too
                List<LogData> sortedInvalidLogs = sortLogsHierarchically(invalidLogs);

                ArrayNode invalidLogsArray = mapper.createArrayNode();
                for (LogData log : sortedInvalidLogs) {
                    ObjectNode logNode = createNestedLogNode(mapper, log, blockMap);
                    invalidLogsArray.add(logNode);
                }

                invalidSection.set("logs", invalidLogsArray);
                resultArray.add(invalidSection);
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultArray);

        } catch (Exception e) {
            throw new RuntimeException("Error converting to nested JSON string", e);
        }
    }

    private ObjectNode createNestedBlockNode(ObjectMapper mapper, BlockData block,
                                             Map<String, List<LogData>> validLogsByBlockId,
                                             Map<String, BlockData> blockMap) {
        ObjectNode blockNode = mapper.createObjectNode();
        blockNode.put("blockId", block.getId());
        blockNode.put("blockName", block.getBlockName());
        blockNode.put("parentBlockId", block.getParentBlockId());

        // Get logs for this block
        List<LogData> blockLogs = validLogsByBlockId.getOrDefault(block.getId(), new ArrayList<>());

        // Sort logs hierarchically based on parentLogId
        List<LogData> sortedLogs = sortLogsHierarchically(blockLogs);

        // Convert logs to JSON array with nested blocks
        ArrayNode logsArray = mapper.createArrayNode();
        for (LogData log : sortedLogs) {
            ObjectNode logNode = createNestedLogNode(mapper, log, blockMap);
            logsArray.add(logNode);
        }

        blockNode.set("logs", logsArray);
        return blockNode;
    }

    private ObjectNode createNestedLogNode(ObjectMapper mapper, LogData log, Map<String, BlockData> blockMap) {
        ObjectNode logNode = mapper.createObjectNode();
        logNode.put("id", log.getId());
        logNode.put("blockId", log.getBlockId());
        logNode.put("parentLogId", log.getParentLogId());
        logNode.put("logType", log.getLogType() != null ? log.getLogType().toString() : null);
        logNode.put("message", log.getMessage());
        logNode.put("referencedBlock", log.getReferencedBlock());
        logNode.put("timestamp", log.getTimestamp());

        // Handle sub_block_start logs by nesting the referenced block
        if (log.getLogType() != null && "sub_block_start".equals(log.getLogType().toString())
                && log.getReferencedBlock() != null) {

            BlockData referencedBlock = blockMap.get(log.getReferencedBlock());

            if (referencedBlock != null) {
                // Create nested block structure
                ObjectNode nestedBlockNode = mapper.createObjectNode();
                nestedBlockNode.put("blockId", referencedBlock.getId());
                nestedBlockNode.put("blockName", referencedBlock.getBlockName());
                nestedBlockNode.put("parentBlockId", referencedBlock.getParentBlockId());

                // Get logs for the referenced block
                List<LogData> referencedBlockLogs = logs.stream()
                        .filter(l -> referencedBlock.getId().equals(l.getBlockId()))
                        .collect(Collectors.toList());

                // Sort logs hierarchically
                List<LogData> sortedReferencedLogs = sortLogsHierarchically(referencedBlockLogs);

                // Convert referenced block logs to JSON array (recursive nesting)
                ArrayNode referencedLogsArray = mapper.createArrayNode();
                for (LogData refLog : sortedReferencedLogs) {
                    ObjectNode refLogNode = createNestedLogNode(mapper, refLog, blockMap);
                    referencedLogsArray.add(refLogNode);
                }

                nestedBlockNode.set("logs", referencedLogsArray);
                logNode.set("nestedBlock", nestedBlockNode);
            } else {
                // Referenced block doesn't exist
                ObjectNode invalidBlockNode = mapper.createObjectNode();
                invalidBlockNode.put("blockId", log.getReferencedBlock());
                invalidBlockNode.put("blockName", "INVALID_BLOCK");
                invalidBlockNode.put("error", "Referenced block not found");
                logNode.set("nestedBlock", invalidBlockNode);
            }
        }

        return logNode;
    }

    private ObjectNode createLogNode(ObjectMapper mapper, LogData log) {
        ObjectNode logNode = mapper.createObjectNode();
        logNode.put("id", log.getId());
        logNode.put("blockId", log.getBlockId());
        logNode.put("parentLogId", log.getParentLogId());
        logNode.put("logType", log.getLogType() != null ? log.getLogType().toString() : null);
        logNode.put("message", log.getMessage());
        logNode.put("referencedBlock", log.getReferencedBlock());
        logNode.put("timestamp", log.getTimestamp());
        return logNode;
    }

    private List<LogData> sortLogsHierarchically(List<LogData> logs) {
        if (logs.isEmpty()) {
            return new ArrayList<>();
        }

        // Create a map for quick lookup
        Map<String, LogData> logMap = logs.stream()
                .collect(Collectors.toMap(LogData::getId, log -> log));

        // Find root logs (those with null or empty parentLogId)
        List<LogData> rootLogs = logs.stream()
                .filter(log -> log.getParentLogId() == null || log.getParentLogId().isEmpty())
                .sorted(Comparator.comparing(LogData::getTimestamp))
                .toList();

        // Build the hierarchical structure
        List<LogData> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (LogData root : rootLogs) {
            addLogAndChildren(root, logMap, result, visited);
        }

        // Add any remaining logs that weren't part of the hierarchy
        for (LogData log : logs) {
            if (!visited.contains(log.getId())) {
                result.add(log);
            }
        }

        return result;
    }

    private void addLogAndChildren(LogData log, Map<String, LogData> logMap, List<LogData> result, Set<String> visited) {
        if (visited.contains(log.getId())) {
            return;
        }
        visited.add(log.getId());
        result.add(log);
        // Find and add children
        List<LogData> children = logMap.values().stream()
                .filter(child -> log.getId().equals(child.getParentLogId()))
                .sorted(Comparator.comparing(LogData::getTimestamp))
                .toList();

        for (LogData child : children) {
            addLogAndChildren(child, logMap, result, visited);
        }
    }
}