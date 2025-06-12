package dev.kuku.vfl.exceptions;

import dev.kuku.vfl.VflBlockDataType;

public class NoPostBlockMessageException extends RuntimeException {
    private final VflBlockDataType block;

    public NoPostBlockMessageException(VflBlockDataType block) {
        this.block = block;
    }
}
