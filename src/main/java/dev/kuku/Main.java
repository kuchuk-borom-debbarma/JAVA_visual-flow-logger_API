package dev.kuku;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotation;

import java.util.concurrent.Executors;

import static dev.kuku.vfl.Foo.publicFoo;
//TODO return values support
public class Main {
    public static void main(String... args) {
        ThreadVFLAnnotation.initialise(createBuffer("main"));
        publicFoo();
    }

    static VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + Main.class.getSimpleName() + "/" + fileName + ".json");
        return new AsyncVFLBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

}