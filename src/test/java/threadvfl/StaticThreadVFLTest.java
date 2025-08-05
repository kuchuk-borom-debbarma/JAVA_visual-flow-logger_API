package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.StaticThreadVFL;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class StaticThreadVFLTest {
    int square(int a) {
        StaticThreadVFL.Log("Squaring " + a);
        return StaticThreadVFL.LogFn(() -> a * a, integer -> "Square of " + a + " = " + integer);
    }

    VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + StaticThreadVFLTest.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncVFLBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void flatFlow() {
        ThreadVFLRunner.StartVFL("Flat FLow", createBuffer("flatFlow.json"), () -> {
            StaticThreadVFL.Log("Starting flat flow right now");
            int a = StaticThreadVFL.LogFn(() -> 1 * 2, integer -> "Output is " + integer);
            StaticThreadVFL.Log("Int a = " + a);
        });
    }

    @Test
    void nestedFlow() {

    }
}
