package dev.kuku.vfl.scopedVFLogger.fluentApi;

public interface RunBlockStep {
    /**
     * Run with default name and no message
     */
    void run();

    RunBlockStep withBlockName(String blockName);

    RunBlockStep withMsg(String message);
}