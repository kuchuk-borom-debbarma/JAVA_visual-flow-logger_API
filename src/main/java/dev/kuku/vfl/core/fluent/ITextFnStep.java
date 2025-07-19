package dev.kuku.vfl.core.fluent;

public interface ITextFnStep<R> {
    R msg();

    R warn();

    R error();
}