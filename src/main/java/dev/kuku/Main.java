package dev.kuku;

import dev.kuku.vfl.VFL;
import dev.kuku.vfl.buffer.SynchronousBuffer;

public class Main {
    static VFL vfl = new VFL(new SynchronousBuffer(10000));

    public static void main(String... args) {
        new SimpleFlow().start();
    }

    static class SimpleFlow {
        void start() {
            vfl.start("Simple Test", logger -> {
                var b = logger.processWriter(l -> {
                    int a = 10 + 2;
                    return null;
                }).writeProcess("GGEZ");
            });
        }
    }
}

