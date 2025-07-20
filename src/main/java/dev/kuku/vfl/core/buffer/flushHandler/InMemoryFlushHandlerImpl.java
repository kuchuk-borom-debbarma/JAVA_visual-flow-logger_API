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

                        // Create a map for BLOCK_END logs by blockId
                        Map<String, LogData> blockEndLogMap = logs.stream()
                                .filter(log -> log.getLogType() != null && "BLOCK_END".equals(log.getLogType().toString()))
                                .collect(Collectors.toMap(LogData::getBlockId, log -> log, (existing, replacement) -> replacement));

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
                                ObjectNode blockNode = createNestedBlockNode(mapper, block, validLogsByBlockId, blockMap, blockEndLogMap);
                                resultArray.add(blockNode);
                            }
                        }

                        // Add an invalid logs section if there are any
                        if (!invalidLogs.isEmpty()) {
                            ObjectNode invalidSection = mapper.createObjectNode();
                            invalidSection.put("blockId", "INVALID_LOGS");
                            invalidSection.put("blockName", "Invalid Logs");
                            invalidSection.putNull("parentBlockId");

                            ArrayNode invalidLogsArray = buildNestedLogHierarchy(mapper, invalidLogs, blockMap, validLogsByBlockId, blockEndLogMap);
                            invalidSection.set("logs", invalidLogsArray);
                            resultArray.add(invalidSection);
                        }

                        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultArray);

                    } catch (Exception e) {
                        throw new RuntimeException("Error converting to nested JSON string", e);
                    }
                }

                private ArrayNode buildLogHierarchy(ObjectMapper mapper, List<LogData> logs) {
                    ArrayNode rootArray = mapper.createArrayNode();

                    if (logs.isEmpty()) {
                        return rootArray;
                    }

                    // Create maps for quick lookup
                    Map<String, LogData> logMap = logs.stream()
                            .collect(Collectors.toMap(LogData::getId, log -> log));

                    // Group logs by their parentLogId
                    Map<String, List<LogData>> logsByParentId = logs.stream()
                            .collect(Collectors.groupingBy(log ->
                                    log.getParentLogId() == null ? "ROOT" : log.getParentLogId()));

                    // Process root logs (those with null parentLogId)
                    List<LogData> rootLogs = logsByParentId.getOrDefault("ROOT", new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(LogData::getTimestamp))
                            .toList();

                    for (LogData rootLog : rootLogs) {
                        ObjectNode logNode = createLogNode(mapper, rootLog);
                        // Recursively add children
                        addChildrenToNode(mapper, logNode, rootLog.getId(), logsByParentId);
                        rootArray.add(logNode);
                    }

                    return rootArray;
                }

                private void addChildrenToNode(ObjectMapper mapper, ObjectNode parentNode, String parentId,
                                               Map<String, List<LogData>> logsByParentId) {
                    List<LogData> children = logsByParentId.getOrDefault(parentId, new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(LogData::getTimestamp))
                            .toList();

                    if (!children.isEmpty()) {
                        ArrayNode childrenArray = mapper.createArrayNode();
                        for (LogData child : children) {
                            ObjectNode childNode = createLogNode(mapper, child);
                            // Recursively add children of this child
                            addChildrenToNode(mapper, childNode, child.getId(), logsByParentId);
                            childrenArray.add(childNode);
                        }
                        parentNode.set("children", childrenArray);
                    }
                }

                private ArrayNode buildNestedLogHierarchy(ObjectMapper mapper, List<LogData> logs,
                                                          Map<String, BlockData> blockMap,
                                                          Map<String, List<LogData>> validLogsByBlockId,
                                                          Map<String, LogData> blockEndLogMap) {
                    ArrayNode rootArray = mapper.createArrayNode();

                    if (logs.isEmpty()) {
                        return rootArray;
                    }

                    // Create maps for quick lookup
                    Map<String, LogData> logMap = logs.stream()
                            .collect(Collectors.toMap(LogData::getId, log -> log));

                    // Group logs by their parentLogId
                    Map<String, List<LogData>> logsByParentId = logs.stream()
                            .collect(Collectors.groupingBy(log ->
                                    log.getParentLogId() == null ? "ROOT" : log.getParentLogId()));

                    // Process root logs (those with null parentLogId)
                    List<LogData> rootLogs = logsByParentId.getOrDefault("ROOT", new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(LogData::getTimestamp))
                            .toList();

                    for (LogData rootLog : rootLogs) {
                        ObjectNode logNode = createNestedLogNode(mapper, rootLog, blockMap, validLogsByBlockId, blockEndLogMap);
                        // Recursively add children
                        addNestedChildrenToNode(mapper, logNode, rootLog.getId(), logsByParentId, blockMap, validLogsByBlockId, blockEndLogMap);
                        rootArray.add(logNode);
                    }

                    return rootArray;
                }

                private void addNestedChildrenToNode(ObjectMapper mapper, ObjectNode parentNode, String parentId,
                                                     Map<String, List<LogData>> logsByParentId,
                                                     Map<String, BlockData> blockMap,
                                                     Map<String, List<LogData>> validLogsByBlockId,
                                                     Map<String, LogData> blockEndLogMap) {
                    List<LogData> children = logsByParentId.getOrDefault(parentId, new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(LogData::getTimestamp))
                            .toList();

                    if (!children.isEmpty()) {
                        ArrayNode childrenArray = mapper.createArrayNode();
                        for (LogData child : children) {
                            ObjectNode childNode = createNestedLogNode(mapper, child, blockMap, validLogsByBlockId, blockEndLogMap);
                            // Recursively add children of this child
                            addNestedChildrenToNode(mapper, childNode, child.getId(), logsByParentId, blockMap, validLogsByBlockId, blockEndLogMap);
                            childrenArray.add(childNode);
                        }
                        parentNode.set("children", childrenArray);
                    }
                }

                private ObjectNode createNestedBlockNode(ObjectMapper mapper, BlockData block,
                                                         Map<String, List<LogData>> validLogsByBlockId,
                                                         Map<String, BlockData> blockMap,
                                                         Map<String, LogData> blockEndLogMap) {
                    ObjectNode blockNode = mapper.createObjectNode();
                    blockNode.put("blockId", block.getId());
                    blockNode.put("blockName", block.getBlockName());
                    blockNode.put("parentBlockId", block.getParentBlockId());

                    // Get logs for this block
                    List<LogData> blockLogs = validLogsByBlockId.getOrDefault(block.getId(), new ArrayList<>());

                    // Build hierarchical log structure with branches
                    ArrayNode logsArray = buildNestedLogHierarchy(mapper, blockLogs, blockMap, validLogsByBlockId, blockEndLogMap);
                    blockNode.set("logs", logsArray);

                    return blockNode;
                }

                private ObjectNode createNestedLogNode(ObjectMapper mapper, LogData log,
                                                       Map<String, BlockData> blockMap,
                                                       Map<String, List<LogData>> validLogsByBlockId,
                                                       Map<String, LogData> blockEndLogMap) {
                    ObjectNode logNode = mapper.createObjectNode();
                    logNode.put("id", log.getId());
                    logNode.put("blockId", log.getBlockId());
                    logNode.put("parentLogId", log.getParentLogId());
                    logNode.put("logType", log.getLogType() != null ? log.getLogType().toString() : null);
                    logNode.put("message", log.getMessage());
                    logNode.put("referencedBlock", log.getReferencedBlock());
                    logNode.put("timestamp", log.getTimestamp());

                    // Handle SUB_BLOCK_START logs by nesting the referenced block
                    if (log.getLogType() != null && "SUB_BLOCK_START".equals(log.getLogType().toString())
                            && log.getReferencedBlock() != null) {

                        BlockData referencedBlock = blockMap.get(log.getReferencedBlock());

                        // Find the BLOCK_END log for the referenced block and add end message
                        LogData blockEndLog = blockEndLogMap.get(log.getReferencedBlock());
                        if (blockEndLog != null) {
                            logNode.put("endMessage", blockEndLog.getMessage());
                        }

                        if (referencedBlock != null) {
                            // Create nested block structure
                            ObjectNode nestedBlockNode = mapper.createObjectNode();
                            nestedBlockNode.put("blockId", referencedBlock.getId());
                            nestedBlockNode.put("blockName", referencedBlock.getBlockName());
                            nestedBlockNode.put("parentBlockId", referencedBlock.getParentBlockId());

                            // Get logs for the referenced block
                            List<LogData> referencedBlockLogs = validLogsByBlockId.getOrDefault(referencedBlock.getId(), new ArrayList<>());

                            // Build hierarchical log structure for nested block
                            ArrayNode referencedLogsArray = buildNestedLogHierarchy(mapper, referencedBlockLogs, blockMap, validLogsByBlockId, blockEndLogMap);
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

                @Override
                public String toString() {
                    return "InMemoryFlushHandlerImpl{" +
                            "logs=\n" + logs.stream().map(Object::toString).collect(Collectors.joining("\n")) +
                            ",\n blocks=\n" + blocks.stream().map(Object::toString).collect(Collectors.joining("\n")) +
                            '}';
                }

                public void cleanup() {
                    logs.clear();
                    blocks.clear();
                }
            }