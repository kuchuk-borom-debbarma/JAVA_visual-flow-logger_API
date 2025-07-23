package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryFlushHandlerImpl implements VFLFlushHandler {
    public List<Log> logs = new ArrayList<>();
    public List<Block> blocks = new ArrayList<>();

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        this.logs.addAll(logs);
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        this.blocks.addAll(blocks);
        return true;
    }

    public String toJsonNested() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode resultArray = mapper.createArrayNode();

            // Create maps for quick lookup
            Map<String, Block> blockMap = blocks.stream()
                    .collect(Collectors.toMap(Block::getId, block -> block));

            Set<String> validBlockIds = blocks.stream()
                    .map(Block::getId)
                    .collect(Collectors.toSet());

                        // Create a map for BLOCK_END logs by blockId
                        Map<String, Log> blockEndLogMap = logs.stream()
                                .filter(log -> log.getLogType() != null && "BLOCK_END".equals(log.getLogType().toString()))
                                .collect(Collectors.toMap(Log::getBlockId, log -> log, (existing, replacement) -> replacement));

                        // Separate valid and invalid logs
                        Map<String, List<Log>> validLogsByBlockId = new HashMap<>();
                        List<Log> invalidLogs = new ArrayList<>();

                        for (Log log : logs) {
                            if (log.getBlockId() != null && validBlockIds.contains(log.getBlockId())) {
                                validLogsByBlockId.computeIfAbsent(log.getBlockId(), k -> new ArrayList<>()).add(log);
                            } else {
                                invalidLogs.add(log);
                            }
                        }

                        // Find blocks that are referenced by SUB_BLOCK_START logs
                        Set<String> referencedBlockIds = logs.stream()
                                .filter(log -> log.getLogType() != null && "SUB_BLOCK_START".equals(log.getLogType().toString()))
                                .map(Log::getReferencedBlockId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        // Process only root blocks (blocks that are not referenced by SUB_BLOCK_START logs)
                        for (Block block : blocks) {
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

                private ArrayNode buildLogHierarchy(ObjectMapper mapper, List<Log> logs) {
                    ArrayNode rootArray = mapper.createArrayNode();

                    if (logs.isEmpty()) {
                        return rootArray;
                    }

                    // Create maps for quick lookup
                    Map<String, Log> logMap = logs.stream()
                            .collect(Collectors.toMap(Log::getId, log -> log));

                    // Group logs by their parentLogId
                    Map<String, List<Log>> logsByParentId = logs.stream()
                            .collect(Collectors.groupingBy(log ->
                                    log.getParentLogId() == null ? "ROOT" : log.getParentLogId()));

                    // Process root logs (those with null parentLogId)
                    List<Log> rootLogs = logsByParentId.getOrDefault("ROOT", new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(Log::getTimestamp))
                            .toList();

                    for (Log rootLog : rootLogs) {
                        ObjectNode logNode = createLogNode(mapper, rootLog);
                        // Recursively add children
                        addChildrenToNode(mapper, logNode, rootLog.getId(), logsByParentId);
                        rootArray.add(logNode);
                    }

                    return rootArray;
                }

                private void addChildrenToNode(ObjectMapper mapper, ObjectNode parentNode, String parentId,
                                               Map<String, List<Log>> logsByParentId) {
                    List<Log> children = logsByParentId.getOrDefault(parentId, new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(Log::getTimestamp))
                            .toList();

                    if (!children.isEmpty()) {
                        ArrayNode childrenArray = mapper.createArrayNode();
                        for (Log child : children) {
                            ObjectNode childNode = createLogNode(mapper, child);
                            // Recursively add children of this child
                            addChildrenToNode(mapper, childNode, child.getId(), logsByParentId);
                            childrenArray.add(childNode);
                        }
                        parentNode.set("children", childrenArray);
                    }
                }

                private ArrayNode buildNestedLogHierarchy(ObjectMapper mapper, List<Log> logs,
                                                          Map<String, Block> blockMap,
                                                          Map<String, List<Log>> validLogsByBlockId,
                                                          Map<String, Log> blockEndLogMap) {
                    ArrayNode rootArray = mapper.createArrayNode();

                    if (logs.isEmpty()) {
                        return rootArray;
                    }

                    // Create maps for quick lookup
                    Map<String, Log> logMap = logs.stream()
                            .collect(Collectors.toMap(Log::getId, log -> log));

                    // Group logs by their parentLogId
                    Map<String, List<Log>> logsByParentId = logs.stream()
                            .collect(Collectors.groupingBy(log ->
                                    log.getParentLogId() == null ? "ROOT" : log.getParentLogId()));

                    // Process root logs (those with null parentLogId)
                    List<Log> rootLogs = logsByParentId.getOrDefault("ROOT", new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(Log::getTimestamp))
                            .toList();

                    for (Log rootLog : rootLogs) {
                        ObjectNode logNode = createNestedLogNode(mapper, rootLog, blockMap, validLogsByBlockId, blockEndLogMap);
                        // Recursively add children
                        addNestedChildrenToNode(mapper, logNode, rootLog.getId(), logsByParentId, blockMap, validLogsByBlockId, blockEndLogMap);
                        rootArray.add(logNode);
                    }

                    return rootArray;
                }

                private void addNestedChildrenToNode(ObjectMapper mapper, ObjectNode parentNode, String parentId,
                                                     Map<String, List<Log>> logsByParentId,
                                                     Map<String, Block> blockMap,
                                                     Map<String, List<Log>> validLogsByBlockId,
                                                     Map<String, Log> blockEndLogMap) {
                    List<Log> children = logsByParentId.getOrDefault(parentId, new ArrayList<>())
                            .stream()
                            .sorted(Comparator.comparing(Log::getTimestamp))
                            .toList();

                    if (!children.isEmpty()) {
                        ArrayNode childrenArray = mapper.createArrayNode();
                        for (Log child : children) {
                            ObjectNode childNode = createNestedLogNode(mapper, child, blockMap, validLogsByBlockId, blockEndLogMap);
                            // Recursively add children of this child
                            addNestedChildrenToNode(mapper, childNode, child.getId(), logsByParentId, blockMap, validLogsByBlockId, blockEndLogMap);
                            childrenArray.add(childNode);
                        }
                        parentNode.set("children", childrenArray);
                    }
                }

                private ObjectNode createNestedBlockNode(ObjectMapper mapper, Block block,
                                                         Map<String, List<Log>> validLogsByBlockId,
                                                         Map<String, Block> blockMap,
                                                         Map<String, Log> blockEndLogMap) {
                    ObjectNode blockNode = mapper.createObjectNode();
                    blockNode.put("blockId", block.getId());
                    blockNode.put("blockName", block.getBlockName());
                    blockNode.put("parentBlockId", block.getParentBlockId());

                    // Get logs for this block
                    List<Log> blockLogs = validLogsByBlockId.getOrDefault(block.getId(), new ArrayList<>());

                    // Build hierarchical log structure with branches
                    ArrayNode logsArray = buildNestedLogHierarchy(mapper, blockLogs, blockMap, validLogsByBlockId, blockEndLogMap);
                    blockNode.set("logs", logsArray);

                    return blockNode;
                }

                private ObjectNode createNestedLogNode(ObjectMapper mapper, Log log,
                                                       Map<String, Block> blockMap,
                                                       Map<String, List<Log>> validLogsByBlockId,
                                                       Map<String, Log> blockEndLogMap) {
                    ObjectNode logNode = mapper.createObjectNode();
                    logNode.put("id", log.getId());
                    logNode.put("blockId", log.getBlockId());
                    logNode.put("parentLogId", log.getParentLogId());
                    logNode.put("logType", log.getLogType() != null ? log.getLogType().toString() : null);
                    logNode.put("message", log.getMessage());
                    logNode.put("referencedBlock", log.getReferencedBlockId());
                    logNode.put("timestamp", log.getTimestamp());

                    // Handle SUB_BLOCK_START logs by nesting the referenced block
                    if (log.getLogType() != null && "SUB_BLOCK_START".equals(log.getLogType().toString())
                            && log.getReferencedBlockId() != null) {

                        Block referencedBlock = blockMap.get(log.getReferencedBlockId());

                        // Find the BLOCK_END log for the referenced block and add end message
                        Log blockEndLog = blockEndLogMap.get(log.getReferencedBlockId());
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
                            List<Log> referencedBlockLogs = validLogsByBlockId.getOrDefault(referencedBlock.getId(), new ArrayList<>());

                            // Build hierarchical log structure for nested block
                            ArrayNode referencedLogsArray = buildNestedLogHierarchy(mapper, referencedBlockLogs, blockMap, validLogsByBlockId, blockEndLogMap);
                            nestedBlockNode.set("logs", referencedLogsArray);
                            logNode.set("nestedBlock", nestedBlockNode);
                        } else {
                            // Referenced block doesn't exist
                            ObjectNode invalidBlockNode = mapper.createObjectNode();
                            invalidBlockNode.put("blockId", log.getReferencedBlockId());
                            invalidBlockNode.put("blockName", "INVALID_BLOCK");
                            invalidBlockNode.put("error", "Referenced block not found");
                            logNode.set("nestedBlock", invalidBlockNode);
                        }
                    }

                    return logNode;
                }

                private ObjectNode createLogNode(ObjectMapper mapper, Log log) {
                    ObjectNode logNode = mapper.createObjectNode();
                    logNode.put("id", log.getId());
                    logNode.put("blockId", log.getBlockId());
                    logNode.put("parentLogId", log.getParentLogId());
                    logNode.put("logType", log.getLogType() != null ? log.getLogType().toString() : null);
                    logNode.put("message", log.getMessage());
                    logNode.put("referencedBlock", log.getReferencedBlockId());
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