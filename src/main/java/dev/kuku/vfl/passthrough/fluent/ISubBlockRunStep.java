package dev.kuku.vfl.passthrough.fluent;

public interface ISubBlockRunStep {
    void run();

    IAsyncFnStep asAsync();
}
