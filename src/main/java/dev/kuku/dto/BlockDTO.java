package dev.kuku.dto;

public class BlockDTO {
    public String id;
    public String blockName;
    public String parentBlockId;

    public BlockDTO(String id, String blockName, String parentBlockId) {
        this.id = id;
        this.blockName = blockName;
        this.parentBlockId = parentBlockId;
    }

    public BlockDTO() {
    } //Required for jackson
}
