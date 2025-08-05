package dev.kuku;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.impl.threadlocal.VFLBlock;

public class Main {
    private static VFLBuffer globalBuffer;

    static {
        VFLBootstrap.init();
    }

    public static void main(String... args) {
        foo();
    }

    @VFLBlock
    static void foo() {
        System.out.println("hello");
        bar();
    }

    @VFLBlock
    static void bar() {
        System.out.println("bar");
    }
}