package dev.kuku.vfl.scoped.fluent;

public interface IRunBlockStep {
    /**
     * Run with default name and no message
     */
    void run();

    IRunBlockStep withBlockName(String blockName);

    IRunBlockStep withMsg(String message);
}