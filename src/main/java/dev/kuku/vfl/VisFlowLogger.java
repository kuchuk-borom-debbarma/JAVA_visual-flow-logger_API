package dev.kuku.vfl;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class VisFlowLogger {
    /// Represents the logger's current block
    private final VflBlockDataType block;
    private final VisFlowLogBuffer buffer;
    private String lastLogId;

    public VisFlowLogger(VflBlockDataType block, VisFlowLogBuffer buffer) {
        this.block = block;
        this.buffer = buffer;
    }


    public void log(String message) {
        String logId = UUID.randomUUID().toString();
        VflLogDataType log = new VflLogDataType(logId, block.getId(), lastLogId, VflLogType.MESSAGE, message, null, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(log);
        lastLogId = logId;
    }

    private <T> void logWithBlock(String blockName, String preFnMessage, Function<T, String> postFnMessage, T result, String subBlockId, String preLogId) {
        if (postFnMessage != null) {
            try {
                String postFnMessageResult = postFnMessage.apply(result);
                if (postFnMessageResult != null) {
                    String postLogId = UUID.randomUUID().toString();
                    VflLogDataType postLog = new VflLogDataType(postLogId, block.getId(), lastLogId, VflLogType.SUB_BLOCK_END, postFnMessageResult, Set.of(subBlockId), Instant.now().toEpochMilli());
                    buffer.pushLogToBuffer(postLog);
                }
            } catch (Exception e) {
                VflLogDataType exceptionLog = new VflLogDataType(UUID.randomUUID().toString(), this.block.getId(), preLogId, VflLogType.SUB_BLOCK_EXCEPTION, e.getMessage(), Set.of(subBlockId), Instant.now().toEpochMilli());
                buffer.pushLogToBuffer(exceptionLog);
            }
        }
    }

    private String[] setupSubBlock(String blockName, String preFnMessage, VflLogType logType) {
        String preLogId = UUID.randomUUID().toString();
        String subBlockId = UUID.randomUUID().toString();
        VflLogDataType preLog = new VflLogDataType(preLogId, block.getId(), lastLogId, logType, preFnMessage, Set.of(subBlockId), Instant.now().toEpochMilli());
        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        buffer.pushBlockToBuffer(subBlock);
        buffer.pushLogToBuffer(preLog);
        lastLogId = preLogId;
        return new String[]{preLogId, subBlockId};
    }

    public <T> void log(String blockName, String preFnMessage, Function<T, String> postFnMessage, Function<VisFlowLogger, T> fn) {
        String[] blockInfo = setupSubBlock(blockName, preFnMessage, VflLogType.SUB_BLOCK_START);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        T result = fn.apply(new VisFlowLogger(subBlock, buffer));

        logWithBlock(blockName, preFnMessage, postFnMessage, result, subBlockId, preLogId);
    }

    public void log(String blockName, String preFnMessage, Function<Void, String> postFnMessage, Consumer<VisFlowLogger> fn) {
        String[] blockInfo = setupSubBlock(blockName, preFnMessage, VflLogType.SUB_BLOCK_START);
        String preLogId = blockInfo[0];
        String subBlockId = blockInfo[1];

        VflBlockDataType subBlock = new VflBlockDataType(block.getId(), subBlockId, blockName);
        fn.accept(new VisFlowLogger(subBlock, buffer));

        logWithBlock(blockName, preFnMessage, postFnMessage, null, subBlockId, preLogId);
    }

    public VisFlowLogger branchOff(String branchName, String message) {
        String[] blockInfo = setupSubBlock(branchName, message, VflLogType.BRANCH);
        String preLogId = blockInfo[0];
        String branchBlockId = blockInfo[1];

        VflBlockDataType branchBlock = new VflBlockDataType(block.getId(), branchBlockId, branchName);
        return new VisFlowLogger(branchBlock, buffer);
    }

    //TODO showing async operation that joins back is left
}