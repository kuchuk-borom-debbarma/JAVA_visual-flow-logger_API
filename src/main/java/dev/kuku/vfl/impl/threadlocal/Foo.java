package dev.kuku.vfl.impl.threadlocal;

public class Foo {
    @VFLBlock
    public static void foo() {
        //their code
        bar();
    }

    @VFLBlock
    public static void bar() {
        //Their code
    }
}
