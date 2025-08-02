package dev.kuku.vfl.core.models.dtos;

public class BlockEndData {
    private Long timestamp;
    private String data;

    // Default constructor for Jackson
    public BlockEndData() {
    }

    public BlockEndData(Long timestamp, String data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    // Getters and setters
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}