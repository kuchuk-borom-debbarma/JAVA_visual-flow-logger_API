package dev.kuku.vfl;

import java.util.Optional;

class VflBlock {
    private final String parentBlockId;
    private final String id;
    private final String blockName;

    public VflBlock(String parentBlockId, String id, String blockName) {
        if (id == null || blockName == null) {
            throw new IllegalArgumentException("id and blockName can not be null");
        }
        this.parentBlockId = parentBlockId;
        this.id = id;
        this.blockName = blockName;
    }

    public Optional<String> getParentBlockId() {
        return Optional.ofNullable(parentBlockId);
    }

    public String getId() {
        return id;
    }

    public String getBlockName() {
        return blockName;
    }
}