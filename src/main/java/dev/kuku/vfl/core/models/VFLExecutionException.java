package dev.kuku.vfl.core.models;

public class VFLExecutionException extends RuntimeException {
    public VFLExecutionException(Exception cause) {
        super(cause.getMessage(), cause);
        // Preserve the original stack trace
        this.setStackTrace(cause.getStackTrace());
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Don't fill in stack trace to preserve the original
        return this;
    }
}
