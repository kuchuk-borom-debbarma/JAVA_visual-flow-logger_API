package dev.kuku;

import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.variants.thread_local.FluentThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;

import java.util.concurrent.Executors;

public class Main {

    public static void main(String... args) {
        //TODO simple annotation based logging.
        //TODO logger can take string and args of param to fill in those {}
        //TODO configuration using spring or file {@link https://claude.ai/chat/28eca1e0-9d4a-4465-836a-5d1feed5a3c4}
        var a = new ThreadSafeInMemoryFlushHandlerImpl();
        ThreadVFL.Runner.Instance.StartVFL("Main", new ThreadSafeAsyncVFLBuffer(1, 5000, a, Executors.newVirtualThreadPerTaskExecutor()), () -> {
            FluentThreadVFL.Call(() -> 1 * 2).asLog(integer -> "Result is {}");
        });
        System.out.println(a.generateNestedJsonStructure());
    }
}
