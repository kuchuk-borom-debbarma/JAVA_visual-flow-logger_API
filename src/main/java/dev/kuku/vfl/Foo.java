package dev.kuku.vfl;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.impl.threadlocal.VFLAnnotationCompletableFuture;
import dev.kuku.vfl.impl.threadlocal.VFLBlock;

import java.util.concurrent.ExecutionException;

public class Foo {
    public static void publicFoo() {
        foo();
    }

    @VFLBlock
    private static void foo() {
        //their code
        bar();
        var t1 = VFLAnnotationCompletableFuture.runAsync(() -> {
            bar();
        });
        var t2 = VFLAnnotationCompletableFuture.runAsync(() -> {
            bar();
        });
        try {
            t1.get();
            t2.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        bar();
    }

    @VFLBlock
    public static void bar() {
        //Their code
        System.out.println("Bar in " + Util.getThreadInfo());
    }
}
