package dev.kuku;

import dev.kuku.vfl.core.buffer.DummyBuffer;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotation;

public class Main {
    public static void main(String... args) {
        ThreadVFLAnnotation.initialise(new DummyBuffer());
    }
}