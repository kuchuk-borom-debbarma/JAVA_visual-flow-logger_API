package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class ScopedFluent {
    private static final ScopedVFL logger = ScopedVFLImpl.get();
    public static TxtStep text = new TxtStep();

    private ScopedFluent() {
    }

    public static RunBlockStep subBlockRun(Runnable runnable) {
        return new RunBlockStepImpl(runnable);
    }

    public static <R> CallBlockStep<R> subBlockCall(Callable<R> callable) {
        return new CallBlockStepImpl<>(callable);
    }

    public static class TxtStep {

        public void error(String error) {
            ScopedVFLImpl.get().error(error);
        }

        public void warn(String warn) {
            ScopedVFLImpl.get().warn(warn);
        }

        public void msg(String msg) {
            ScopedVFLImpl.get().msg(msg);
        }

        public <R> FnMsg<R> fn(Callable<R> callable) {
            return new FnMsg<>(callable);
        }

        public static class FnMsg<R> {
            private final Callable<R> callable;

            public FnMsg(Callable<R> callable) {
                this.callable = callable;
            }

            public R msg(Function<R, String> msgFn) {
                return ScopedVFLImpl.get().msgFn(callable, msgFn);
            }
        }
    }
}

