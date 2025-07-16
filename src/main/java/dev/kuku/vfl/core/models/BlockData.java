package dev.kuku.vfl.core.models;

public class BlockData {
    private String id;
    private String parentBlockId;
    private String blockName;

    // No-args constructor for Jackson
    public BlockData() {
    }

    public BlockData(String id, String parentBlockId, String blockName) {
        this.id = id;
        this.parentBlockId = parentBlockId;
        this.blockName = blockName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentBlockId() {
        return parentBlockId;
    }

    public void setParentBlockId(String parentBlockId) {
        this.parentBlockId = parentBlockId;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    @Override
    public String toString() {
        return "BlockData{" +
                "id='" + id + '\'' +
                ", parentBlockId='" + parentBlockId + '\'' +
                ", blockName='" + blockName + '\'' +
                '}';
    }
}