package dev.kuku.vfl.internal.dao;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryVflDaoImpl implements VflDao {
    private static final Logger log = LoggerFactory.getLogger(InMemoryVflDaoImpl.class);
    //Key = ID, Value = Block
    private final Map<String, VflBlockDataType> blocks = new HashMap<>();

    @Override
    public void upsertBlocks(List<VflBlockDataType> blocksToAdd) {
        //Create map of blocksToAdd for fast lookup required in the loop below.
        Map<String, VflBlockDataType> blocksToAddMap = blocksToAdd.stream()
                .collect(Collectors.toMap(VflBlockDataType::getId, b -> b));
        //Parent validation iteration.
        for (VflBlockDataType block : blocksToAdd) {
            //If block is already stored, skip.
            if (blocks.containsKey(block.getId())) continue;
            //Check if parentBlockId is present in list or in blocksToAdd
            if (block.getParentBlockId().isPresent()) {
                String parentBlockId = block.getParentBlockId().get();
                if (!blocks.containsKey(parentBlockId) && !blocksToAddMap.containsKey(parentBlockId)) {
                    log.error("Parent block not found for block {}, ParentID = {}", block.getId(), parentBlockId);
                    throw new RuntimeException("Parent block not found for block " + block.getId());
                }
            }
        }
        //All blocks are valid as no exceptions were thrown.
        blocksToAdd.forEach(b -> {
            if (!blocks.containsKey(b.getId())) {
                blocks.put(b.getId(), b);
            }
        });
    }

    @Override
    public void upsertLogs(List<VflLogDataType> logs) {

    }
}
