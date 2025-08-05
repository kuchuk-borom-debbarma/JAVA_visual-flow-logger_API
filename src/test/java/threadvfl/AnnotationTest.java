package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
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

    @VFLBlock
    void foo() {
        System.out.println("Hello World");
        bar();
    }

    @VFLBlock
    void bar() {
        System.out.println("Bar called");
    }

    @Test
    void nest() {
        foo();
        b.flushAndClose();
    }
}
