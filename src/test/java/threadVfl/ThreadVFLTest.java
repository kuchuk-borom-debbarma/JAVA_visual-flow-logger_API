package threadVfl;

import dev.kuku.vfl.core.buffer.SynchronousVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.core.models.VFLExecutionException;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ThreadVFLTest {
    VFLBuffer b = new SynchronousVFLBuffer(10, new NestedJsonFlushHandler("test/output/threadVFL/gg.json"));

    @Test
    void nestedMultiThreadTest() {
        ThreadVFLRunner.StartVFL("Multi thread test", b, () -> {
            //So this works
            ThreadVFL.getCurrentLogger().log("Running in thread " + Thread.currentThread().threadId());
            ThreadVFL.getCurrentLogger().log("Running sum same thread");
            int ss = ThreadVFL.getCurrentLogger().callPrimarySubBlock("Sum main", null, () -> square(2), null);
            System.out.println(ss);
            ThreadVFL.getCurrentLogger().log("Finished main sum " + ss);
            var s = ThreadVFL.getCurrentLogger().callPrimarySubBlock("Sum another thread", null, () -> CompletableFuture.supplyAsync(() -> asyncSquare(2)
                    , Executors.newSingleThreadExecutor(r -> {
                        var t = new Thread(r);
                        t.setName("GGEZ");
                        return t;
                    })), null);
            try {
                s.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new VFLExecutionException(e);
            }
            ThreadVFL.getCurrentLogger().log("Finished sum another thread");
        });
    }

    @Test
    void threadPoolTest() {
        var e = Executors.newSingleThreadExecutor(r -> {
            var t = new Thread(r);
            t.setName("GGEZ");
            return t;
        });
        ThreadVFLRunner.StartVFL("Threadpool test", b, () -> {
            ThreadVFL.getCurrentLogger().log("Running in thread " + Thread.currentThread().threadId());
            var a = ThreadVFL.getCurrentLogger().callPrimarySubBlock("t1", null, () -> CompletableFuture.supplyAsync(() -> square(2), e), null);
            var b = ThreadVFL.getCurrentLogger().callPrimarySubBlock("t2", null, () -> CompletableFuture.supplyAsync(() -> square(2), e), null);
            var c = ThreadVFL.getCurrentLogger().callPrimarySubBlock("t3", null, () -> CompletableFuture.supplyAsync(() -> square(2), e), null);
            try {
                a.get();
                b.get();
                c.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    private int square(int x) {
        System.out.println("Executor thread stack size: " + ThreadVFL.loggerStack.get().size());
        System.out.println("Current logger block name: " + ThreadVFL.getCurrentLogger().ctx.blockInfo.getBlockName() + " and id " + ThreadVFL.getCurrentLogger().ctx.blockInfo.getId());
        ThreadVFL.getCurrentLogger().log("Running in thread " + Thread.currentThread().threadId());
        return x * x;
    }


    int asyncSquare(int a) {
        square(a);
        ThreadVFL.getCurrentLogger().log("Another thread starting ...");
        ThreadVFL.getCurrentLogger().callPrimarySubBlock("nested thead", null, () -> CompletableFuture.supplyAsync(() -> square(a),
                Executors.newSingleThreadExecutor(r -> {
                    var t = new Thread(r);
                    t.setName("NESTED");
                    return t;
                })), null);
        ThreadVFL.getCurrentLogger().log("Another thread Complete " + Thread.currentThread().threadId());
        return 1;
    }
}