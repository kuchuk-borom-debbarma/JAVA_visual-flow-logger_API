package dev.kuku;

import dev.kuku.vfl.core.buffer.VFLBuffer;

import static dev.kuku.vfl.impl.threadlocal.VFLInstrumentation.foo;

public class Main {
    private static VFLBuffer globalBuffer;


    public static void main(String... args) {
        foo();
        foo();
        foo();
    }


}