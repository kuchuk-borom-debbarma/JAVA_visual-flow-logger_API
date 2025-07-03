package dev.kuku;

import dev.kuku.vfl.VFL;
import dev.kuku.vfl.buffer.SynchronousBuffer;

import java.util.function.Function;

public class Main {
    static VFL vfl = new VFL(new SynchronousBuffer(10000));

    public static void main(String... args) {
        new SimpleFlow().start();
        tester(Main::test);
    }

    public static String test(String string) {
        System.out.println(string);
        return string;
    }

    public static void tester(Function<String,String> endMessageFn) {
        System.out.println(endMessageFn.toString());
    }

    static class SimpleFlow {
        void start() {

        }
    }
}

