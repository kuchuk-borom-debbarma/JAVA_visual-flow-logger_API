package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VFLFlushHandler implementation that generates nested JSON output showing hierarchical execution flow.
 * Creates a single JSON file with blocks and their nested log chains based on parent-child relationships.
 * NOT RECOMMENDED for production. Only use during development and testing.
 */
public class NestedJsonFlushHandler implements VFLFlushHandler {

    private final String outputFilePath;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timeFormatter;

    // Data storage
    private final Map<String, Block> blocks = new ConcurrentHashMap<>();
    private final Map<String, Log> logs = new ConcurrentHashMap<>();
    private final Map<String, Long> blockStarts = new ConcurrentHashMap<>();
    private final Map<String, BlockEndData> blockEnds = new ConcurrentHashMap<>();

    public NestedJsonFlushHandler(String outputFilePath) {
        this.outputFilePath = outputFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
    }

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        if (logs != null) {
            for (Log log : logs) {
                this.logs.put(log.getId(), log);
            }
        }
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        if (blocks != null) {
            for (Block block : blocks) {
                this.blocks.put(block.getId(), block);
            }
        }
        return true;
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        if (blockStarts != null) {
            this.blockStarts.putAll(blockStarts);
        }
        return true;
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds) {
        if (blockEnds != null) {
            this.blockEnds.putAll(blockEnds);
        }
        return true;
    }

    @Override
    public void closeFlushHandler() {
        try {
            List<BlockJson> rootBlocks = buildNestedStructure();
            writeJsonToFile(rootBlocks);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate nested JSON output", e);
        }
    }


    private List<BlockJson> buildNestedStructure() {
        // Find root blocks (blocks with no parent)
        List<Block> rootBlocksList = blocks.values().stream()
                .filter(block -> block.getParentBlockId() == null)
                .toList();

        List<BlockJson> result = new ArrayList<>();
        for (Block rootBlock : rootBlocksList) {
            result.add(buildBlockJson(rootBlock));
        }

        return result;
    }

    private BlockJson buildBlockJson(Block block) {
        BlockJson blockJson = new BlockJson();
        blockJson.blockId = block.getId();
        blockJson.parentBlockId = block.getParentBlockId();
        blockJson.name = block.getBlockName();

        // Format times and calculate duration
        Long startTime = blockStarts.get(block.getId());
        if (startTime != null) {
            blockJson.startTime = formatTime(startTime);
        }

        // Handle block end time and calculate duration
        BlockEndData blockEnd = blockEnds.get(block.getId());
        if (blockEnd != null) {
            blockJson.endTime = formatTime(blockEnd.getEndTime());
            blockJson.endMessage = blockEnd.getEndMessage();

            // Calculate duration if both start and end times are available
            if (startTime != null) {
                long duration = blockEnd.getEndTime() - startTime;
                blockJson.duration = formatDuration(duration);
            }
        }

        // Build logs chain for this block
        blockJson.logsChain = buildLogsChain(block.getId(), null);

        return blockJson;
    }

    private List<LogJson> buildLogsChain(String blockId, String parentLogId) {
        // Get all logs for this block with the specified parent log ID
        List<Log> blockLogs = logs.values().stream()
                .filter(log -> Objects.equals(log.getBlockId(), blockId))
                .filter(log -> Objects.equals(log.getParentLogId(), parentLogId))
                .sorted(Comparator.comparing(Log::getTimestamp))
                .toList();

        List<LogJson> logJsons = new ArrayList<>();

        for (Log log : blockLogs) {
            LogJson logJson = new LogJson();
            logJson.id = log.getId();
            logJson.type = log.getLogType().toString();
            logJson.message = log.getMessage();
            logJson.timestamp = formatTime(log.getTimestamp());

            // Handle SubBlockStartLog special case
            if (log instanceof SubBlockStartLog subBlockLog) {
                String referencedBlockId = subBlockLog.getReferencedBlockId();

                // Add duration and end message for sub-block logs
                Long subBlockStartTime = blockStarts.get(referencedBlockId);
                BlockEndData subBlockEnd = blockEnds.get(referencedBlockId);

                if (subBlockStartTime != null && subBlockEnd != null) {
                    long duration = subBlockEnd.getEndTime() - subBlockStartTime;
                    logJson.duration = formatDuration(duration);
                    logJson.endMessage = subBlockEnd.getEndMessage();
                }

                // Add referenced block
                Block referencedBlock = blocks.get(referencedBlockId);
                if (referencedBlock != null) {
                    logJson.referencedBlock = buildBlockJson(referencedBlock);
                }
            }

            // Build nested logs chain (logs that have this log as parent)
            List<LogJson> nestedLogs = buildLogsChain(blockId, log.getId());
            if (!nestedLogs.isEmpty()) {
                logJson.logsChain = nestedLogs;
            }

            logJsons.add(logJson);
        }

        return logJsons;
    }

    private String formatTime(long timestampMillis) {
        return timeFormatter.format(Instant.ofEpochMilli(timestampMillis));
    }

    private String formatDuration(long durationMillis) {
        if (durationMillis < 1000) {
            return durationMillis + "ms";
        } else if (durationMillis < 60000) {
            double seconds = durationMillis / 1000.0;
            return String.format("%.3fs", seconds);
        } else {
            long minutes = durationMillis / 60000;
            long remainingMs = durationMillis % 60000;
            double remainingSeconds = remainingMs / 1000.0;
            return String.format("%dmin %.3fs", minutes, remainingSeconds);
        }
    }

    private void writeJsonToFile(List<BlockJson> rootBlocks) throws IOException {
        File outputFile = new File(outputFilePath);

        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories for: " + outputFilePath);
            }
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            objectMapper.writeValue(writer, rootBlocks);
        }
    }

    // JSON data classes
    private static class BlockJson {
        public String blockId;
        public String parentBlockId;
        public String name;
        public String startTime;
        public String endTime;
        public String duration;
        public String endMessage;
        public List<LogJson> logsChain;
    }

    private static class LogJson {
        public String id;
        public String type;
        public String message;
        public String timestamp;
        public String duration; // Only for SubBlockStartLog
        public String endMessage; // Only for SubBlockStartLog
        public BlockJson referencedBlock; // Only for SubBlockStartLog
        public List<LogJson> logsChain; // Nested logs
    }
}