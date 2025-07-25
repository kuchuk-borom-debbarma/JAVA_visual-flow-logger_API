package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void cleanup() {
        logs.clear();
        blocks.clear();
    }

    public String toNestedJson() throws JsonProcessingException {
        List<Map<String, Object>> nestedBlocks = buildNestedStructure();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper.writeValueAsString(nestedBlocks);
    }

    private List<Map<String, Object>> buildNestedStructure() {
        // Create a map of blocks by ID for quick lookup
        Map<String, Block> blocksById = blocks.stream()
                .collect(Collectors.toMap(Block::getId, block -> block));

        // Group logs by block ID
        Map<String, List<Log>> logsByBlockId = logs.stream()
                .collect(Collectors.groupingBy(Log::getBlockId));

        // Create a map of logs by their parent log ID for efficient lookup
        Map<String, List<Log>> logsByParentId = logs.stream()
                .filter(log -> log.getParentLogId() != null && !log.getParentLogId().isEmpty())
                .collect(Collectors.groupingBy(Log::getParentLogId));

        // Process root blocks (blocks with no parent)
        return blocks.stream()
                .filter(block -> block.getParentBlockId() == null || block.getParentBlockId().isEmpty())
                .map(block -> buildBlockNode(block, blocksById, logsByBlockId, logsByParentId))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildBlockNode(Block block,
                                               Map<String, Block> blocksById,
                                               Map<String, List<Log>> logsByBlockId,
                                               Map<String, List<Log>> logsByParentId) {
        Map<String, Object> blockNode = new HashMap<>();
        blockNode.put("id", block.getId());
        blockNode.put("name", block.getBlockName());

        // Get root logs for this block (logs with no parentLogId or parentLogId matches blockId)
        List<Log> blockLogs = logsByBlockId.getOrDefault(block.getId(), new ArrayList<>());
        List<Log> rootLogs = blockLogs.stream()
                .filter(log -> log.getParentLogId() == null || log.getParentLogId().isEmpty() || log.getParentLogId().equals(block.getId()))
                .toList();

        // Build log tree for this block
        List<Map<String, Object>> logNodes = rootLogs.stream()
                .map(log -> buildLogNode(log, blocksById, logsByParentId))
                .collect(Collectors.toList());

        blockNode.put("logs", logNodes);
        return blockNode;
    }

    private Map<String, Object> buildLogNode(Log log,
                                             Map<String, Block> blocksById,
                                             Map<String, List<Log>> logsByParentId) {
        Map<String, Object> logNode = new HashMap<>();
        logNode.put("id", log.getId());
        logNode.put("type", log.getLogType()); // Simple string value
        logNode.put("message", log.getMessage());

        // Handle referenced blocks for specific log types
        if (log instanceof SubBlockStartLog) {
            String referencedBlockId = ((SubBlockStartLog) log).getReferencedBlockId();
            Block referencedBlock = blocksById.get(referencedBlockId);
            if (referencedBlock != null) {
                Map<String, Object> referencedBlockNode = new HashMap<>();
                referencedBlockNode.put("id", referencedBlock.getId());
                referencedBlockNode.put("name", referencedBlock.getBlockName());
                logNode.put("referencedBlock", referencedBlockNode);
            }
        }

        // Recursively build child logs
        List<Log> childLogs = logsByParentId.getOrDefault(log.getId(), new ArrayList<>());
        if (!childLogs.isEmpty()) {
            List<Map<String, Object>> childNodes = childLogs.stream()
                    .map(childLog -> buildLogNode(childLog, blocksById, logsByParentId))
                    .collect(Collectors.toList());
            logNode.put("children", childNodes);
        }

        return logNode;
    }
}