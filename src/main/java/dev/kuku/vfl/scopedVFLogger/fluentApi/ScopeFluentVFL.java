package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.scopedVFLogger.fluentApi.subBlockStep.SubBlockStep;

public interface ScopeFluentVFL {
    /**
     * Log a message
     *
     * @param msg message to log
     */
    void msg(String msg);

    /**
     * start a sub block
     */
    SubBlockStep subBlock();
}
