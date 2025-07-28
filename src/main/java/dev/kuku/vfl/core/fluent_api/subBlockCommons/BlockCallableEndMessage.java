package dev.kuku.vfl.core.fluent_api.subBlockCommons;

import java.util.function.Function;

public interface BlockCallableEndMessage<R> {
    BlockCallableEndMessage withEndMessageMapper(Function<R, String> endMessageSerializer);
}
