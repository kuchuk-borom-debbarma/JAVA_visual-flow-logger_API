package dev.kuku.vfl;

import dev.kuku.vfl.impl.threadlocal.VFLBlock;

public class Foo {
    public static void publicFoo() {
        foo();
    }

    @VFLBlock
    private static void foo() {
        //their code
        bar();
    }

    @VFLBlock
    public static void bar() {
        //Their code
    }
}
