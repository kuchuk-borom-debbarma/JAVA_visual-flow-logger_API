package dev.kuku.vfl;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class VflBuffer {
    private final Map<String, VflBlock> blocks;
    private final Map<String, VflLog> logs;

    // DateTimeFormatter for human-readable timestamps
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'")
                    .withZone(ZoneOffset.UTC);

    public VflBuffer(int bufferSize) {
        blocks = new HashMap<>();
        logs = new HashMap<>();
    }

    public ImmutableVflBlockOperator createBlock(String parentBlockId, String id, String blockName) {
        if (blocks.containsKey(id)) {
            throw new KeyAlreadyExistsException("Block ID " + id + " already exists");
        }
        var block = new VflBlock(parentBlockId, id, blockName);
        blocks.put(id, block);
        return new ImmutableVflBlockOperator(id, this);
    }


    public VflLog createLog(String logId, String blockId, String parentLogId, VflLogType logType, String logValue, Set<String> blockPointers) {
        if (logs.containsKey(logId)) {
            throw new KeyAlreadyExistsException("Log ID " + logId + " already exists");
        }
        var log = new VflLog(logId, blockId, parentLogId, logType, logValue, blockPointers, Instant.now().toEpochMilli());
        logs.put(logId, log);
        return log;
    }

    /**
     * Converts UTC milliseconds timestamp to human-readable format
     */
    private String formatTimestamp(long timestampMillis) {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestampMillis));
    }

    public String flushToJSON() {
        JSONObject root = new JSONObject();

        // Get blocks in hierarchical order
        List<VflBlock> orderedBlocks = getBlocksInHierarchicalOrder();

        // Add blocks with their logs nested inside
        JSONArray blocksArray = new JSONArray();
        for (VflBlock block : orderedBlocks) {
            JSONObject blockObj = new JSONObject();
            blockObj.put("id", block.getId());
            block.getParentBlockId().ifPresent(parentId -> blockObj.put("parentBlockId", parentId));
            blockObj.put("name", block.getBlockName());

            // Add logs for this block in hierarchical order
            List<VflLog> blockLogs = getLogsForBlockInHierarchicalOrder(block.getId());
            JSONArray logsArray = new JSONArray();
            for (VflLog log : blockLogs) {
                JSONObject logObj = new JSONObject();
                logObj.put("id", log.getId());
                log.getParentLogId().ifPresent(parentId -> logObj.put("parentLogId", parentId));
                logObj.put("type", log.getLogType().name());
                log.getLogValue().ifPresent(value -> logObj.put("value", value));
                log.getBlockPointers().ifPresent(pointers ->
                        logObj.put("blockPointers", new JSONArray(pointers)));

                // Add both raw timestamp and human-readable format
                logObj.put("timestamp", log.getTimeStamp());
                logObj.put("timestampFormatted", formatTimestamp(log.getTimeStamp()));

                logsArray.put(logObj);
            }
            blockObj.put("logs", logsArray);

            blocksArray.put(blockObj);
        }
        root.put("blocks", blocksArray);

        // Add orphaned logs (logs not associated with any existing block)
        JSONArray orphanedLogsArray = new JSONArray();
        logs.values().stream()
                .filter(log -> !blocks.containsKey(log.getBlockId()))
                .forEach(log -> {
                    JSONObject logObj = new JSONObject();
                    logObj.put("id", log.getId());
                    logObj.put("blockId", log.getBlockId());
                    log.getParentLogId().ifPresent(parentId -> logObj.put("parentLogId", parentId));
                    logObj.put("type", log.getLogType().name());
                    log.getLogValue().ifPresent(value -> logObj.put("value", value));
                    log.getBlockPointers().ifPresent(pointers ->
                            logObj.put("blockPointers", new JSONArray(pointers)));

                    // Add both raw timestamp and human-readable format
                    logObj.put("timestamp", log.getTimeStamp());
                    logObj.put("timestampFormatted", formatTimestamp(log.getTimeStamp()));
                });

        if (orphanedLogsArray.length() > 0) {
            root.put("orphanedLogs", orphanedLogsArray);
        }

        return root.toString(4);
    }

    private List<VflBlock> getBlocksInHierarchicalOrder() {
        List<VflBlock> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        // First, add all root blocks (blocks with no parent)
        List<VflBlock> rootBlocks = blocks.values().stream()
                .filter(block -> block.getParentBlockId().isEmpty())
                .collect(Collectors.toList());

        for (VflBlock rootBlock : rootBlocks) {
            addBlockAndChildrenRecursively(rootBlock, result, processed);
        }

        // Add any remaining blocks that might have invalid parent references
        for (VflBlock block : blocks.values()) {
            if (!processed.contains(block.getId())) {
                result.add(block);
            }
        }

        return result;
    }

    private void addBlockAndChildrenRecursively(VflBlock block, List<VflBlock> result, Set<String> processed) {
        if (processed.contains(block.getId())) {
            return; // Avoid infinite loops in case of circular references
        }

        result.add(block);
        processed.add(block.getId());

        // Find and add all children of this block
        List<VflBlock> children = blocks.values().stream()
                .filter(b -> b.getParentBlockId().isPresent() &&
                        b.getParentBlockId().get().equals(block.getId()))
                .collect(Collectors.toList());

        for (VflBlock child : children) {
            addBlockAndChildrenRecursively(child, result, processed);
        }
    }

    private List<VflLog> getLogsForBlockInHierarchicalOrder(String blockId) {
        List<VflLog> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        // Get all logs for this block
        List<VflLog> blockLogs = logs.values().stream()
                .filter(log -> log.getBlockId().equals(blockId))
                .collect(Collectors.toList());

        // First, add all root logs (logs with no parent)
        List<VflLog> rootLogs = blockLogs.stream()
                .filter(log -> log.getParentLogId().isEmpty())
                .collect(Collectors.toList());

        for (VflLog rootLog : rootLogs) {
            addLogAndChildrenRecursively(rootLog, blockLogs, result, processed);
        }

        // Add any remaining logs that might have invalid parent references
        for (VflLog log : blockLogs) {
            if (!processed.contains(log.getId())) {
                result.add(log);
            }
        }

        return result;
    }

    private void addLogAndChildrenRecursively(VflLog log, List<VflLog> allBlockLogs,
                                              List<VflLog> result, Set<String> processed) {
        if (processed.contains(log.getId())) {
            return; // Avoid infinite loops in case of circular references
        }

        result.add(log);
        processed.add(log.getId());

        // Find and add all children of this log within the same block
        List<VflLog> children = allBlockLogs.stream()
                .filter(l -> l.getParentLogId().isPresent() &&
                        l.getParentLogId().get().equals(log.getId()))
                .collect(Collectors.toList());

        for (VflLog child : children) {
            addLogAndChildrenRecursively(child, allBlockLogs, result, processed);
        }
    }
}