package dev.kuku.vfl.core.fluent_api.subBlockCommons;

import java.util.function.Function;

public interface BlockCallableEndMessage<R> extends BlockStartMsg {
    BlockCallableEndMessage withEndMessageMapper(Function<R, String> endMessageSerializer, Object... args);
}
