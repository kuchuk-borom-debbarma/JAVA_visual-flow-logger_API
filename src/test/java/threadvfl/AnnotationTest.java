package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.impl.annotation.Log;
import dev.kuku.vfl.impl.annotation.VFLAnnotationProcessor;
import dev.kuku.vfl.impl.annotation.VFLBlock;
import dev.kuku.vfl.impl.annotation.VFLFutures;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class AnnotationTest {
    static VFLBuffer b;

    static VFLBuffer createBuffer(String fileName) {
        VFLFlushHandler f = new NestedJsonFlushHandler("test/output/" + AnnotationTest.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
        //return new NoOpsBuffer();
    }

    @Test
    void linear() {
        //VFLAnnotationProcessor.initialise(createBuffer("linear"));
        new TestService().linear();
    }

    @Test
    void async() {
        VFLAnnotationProcessor.initialise(createBuffer("async"));
        new TestService().async();
    }

}

class TestService {
    @VFLBlock
    private int square(int a) {
        return a * a;
    }

    @VFLBlock
    private int squareAndMultiply(int a, int b) {
        int num = a * b;
        return square(num);
    }

    @VFLBlock
    public void linear() {
        Log.Info("SUP");
        int a = Log.InfoFn(() -> square(12), "Squaring {} = {}", 12);
        int b = squareAndMultiply(a, 2);
        square(b);
    }

    @VFLBlock
    public void async() {
        Log.Info("SUP");
        var e = Executors.newFixedThreadPool(1);
        var t = VFLFutures.runAsync(() -> square(1), e);
        var t2 = VFLFutures.runAsync(() -> square(1), e);
        var t3 = VFLFutures.runAsync(() -> square(1), e);
        t.join();
        t2.join();
        t3.join();
    }

}
