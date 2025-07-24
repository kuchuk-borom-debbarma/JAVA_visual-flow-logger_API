package dev.kuku.vfl.core.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VflResponse<T> {
    private String message;
    private T data;

}
