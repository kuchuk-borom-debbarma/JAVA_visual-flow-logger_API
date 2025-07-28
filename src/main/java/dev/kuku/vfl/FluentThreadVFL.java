package dev.kuku.vfl;

import dev.kuku.vfl.core.fluent_api.callable.FluentVFLCallable;

public final class FluentThreadVFL {
    public static FluentVFLCallable Get() {
        return new FluentVFLCallable(ThreadVFL.Get());
    }
}
