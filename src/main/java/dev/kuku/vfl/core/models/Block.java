package dev.kuku.vfl.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Block {
    private String id;
    private String parentBlockId;
    private String blockName;
}