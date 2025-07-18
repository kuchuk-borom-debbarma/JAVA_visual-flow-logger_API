package dev.kuku.vfl.scopedVFLogger.fluentApi.subBlockStep;

public interface SubBlockStep {
    /**
     * Run with default name and no message
     *
     * @param runnable method to run
     */
    void run(Runnable runnable);

    /**
     * Call with default name and no message
     */
    <R> CallStep<R> callable();

    /**
     * Set the name of the block
     *
     * @param blockName name of the block
     */
    SubBlockStep blockName(String blockName);

    /**
     * Message for the sub block start
     *
     * @param msg message
     */
    SubBlockStep blockMsg(String msg);
}