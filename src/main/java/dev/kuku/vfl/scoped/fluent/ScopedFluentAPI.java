package dev.kuku.vfl.scoped.fluent;

import dev.kuku.vfl.scoped.IScopedVFL;
import dev.kuku.vfl.scoped.ScopedVFL;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class ScopedFluentAPI {
    private static final IScopedVFL logger = ScopedVFL.get();
    public static TxtStep text = new TxtStep();

    private ScopedFluentAPI() {
    }

    public static IRunBlockStep subBlockRun(Runnable runnable) {
        return new RunBlockStep(runnable);
    }

    public static <R> ICallBlockStep<R> subBlockCall(Callable<R> callable) {
        return new CallBlockStep<>(callable);
    }

    public static class TxtStep {

        public void error(String error) {
            ScopedVFL.get().error(error);
        }

        public void warn(String warn) {
            ScopedVFL.get().warn(warn);
        }

        public void msg(String msg) {
            ScopedVFL.get().msg(msg);
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
                return ScopedVFL.get().msgFn(callable, msgFn);
            }
        }
    }
}

