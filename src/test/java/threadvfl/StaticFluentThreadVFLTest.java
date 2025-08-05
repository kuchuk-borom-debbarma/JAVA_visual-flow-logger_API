package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.StaticFluentThreadVFL;
import dev.kuku.vfl.impl.threadlocal.StaticThreadVFL;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class StaticFluentThreadVFLTest {
    int square(int a) {
        StaticThreadVFL.Log("Squaring " + a);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return StaticThreadVFL.LogFn(() -> a * a, integer -> "Square of " + a + " = " + integer);
    }

    VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + this.getClass().getSimpleName() + "/" + fileName + ".json");
        return new AsyncVFLBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void flatFlow() {
        ThreadVFLRunner.StartVFL("flat flow test", createBuffer("flatFlowTest"), () -> {
            StaticFluentThreadVFL.Log("Starting {} linear flow", this.getClass().getSimpleName());
            StaticFluentThreadVFL.Call(() -> square(2))
                    .asLog("Result is {}");
        });
    }
}
