package dev.kuku.vfl.core;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Fluent API for VFL logger providing intuitive chaining syntax.
 * <p>
 * Usage examples:
 * - fluent.text("Hello World").asMsg()
 * - fluent.text("Something wrong").asError()
 * - fluent.fn(() -> computeValue()).asMsg(result -> "Computed: " + result)
 * - fluent.fn(() -> riskyOperation()).asWarn(result -> "Warning: " + result)
 */
public class VFLFluentAPI {
    private final IVFL logger;

    public VFLFluentAPI(IVFL logger) {
        this.logger = logger;
    }

    /**
     * Start a text-based fluent chain
     */
    public TextStep text(String message) {
        return new TextStep(message);
    }

    /**
     * Start a function-based fluent chain
     */
    public <R> FnStep<R> fn(Callable<R> callable) {
        return new FnStep<>(callable);
    }

    /**
     * Text-based fluent step for simple string messages
     */
    public class TextStep {
        private final String message;

        private TextStep(String message) {
            this.message = message;
        }

        public void asMsg() {
            logger.msg(message);
        }

        public void asWarn() {
            logger.warn(message);
        }

        public void asError() {
            logger.error(message);
        }
    }

    /**
     * Function-based fluent step for executable operations
     */
    public class FnStep<R> {
        private final Callable<R> callable;

        private FnStep(Callable<R> callable) {
            this.callable = callable;
        }

        /**
         * Execute function and log result as message
         */
        public R asMsg(Function<R, String> textFunction) {
            return logger.msgFn(callable, textFunction);
        }

        /**
         * Execute function and log result as warning
         */
        public R asWarn(Function<R, String> textFunction) {
            return logger.warnFn(callable, textFunction);
        }

        /**
         * Execute function and log result as error
         */
        public R asError(Function<R, String> textFunction) {
            return logger.errorFn(callable, textFunction);
        }
    }
}