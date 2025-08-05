package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.StaticFluentThreadVFL;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotation;
import dev.kuku.vfl.impl.threadlocal.VFLBlock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class AnnotationTest {
    static VFLBuffer b;

    static {
        b = createBuffer("nest");
        ThreadVFLAnnotation.initialise(b);
    }

    static VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + AnnotationTest.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncVFLBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void test() {
        new TestService().linear();
    }
}

class TestService {
    @VFLBlock
    private int square(int a) {
        StaticFluentThreadVFL.Log("Squaring {}", a);
        return a * a;
    }

    @VFLBlock
    private int squareAndMultiply(int a, int b) {
        StaticFluentThreadVFL.Log("SquaringAndMultiply {} {}", a, b);
        int num = a * b;
        return square(num);
    }

    @VFLBlock
    public void linear() {
        int a = square(10);
        int b = squareAndMultiply(a, 2);
        square(b);
    }
}
