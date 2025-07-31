package dev.kuku;

import dev.kuku.vfl.core.buffer.ThreadSafeAsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.ThreadSafeInMemoryFlushHandlerImpl;
import dev.kuku.vfl.variants.thread_local.FluentThreadVFL;
import dev.kuku.vfl.variants.thread_local.ThreadVFL;

import java.util.concurrent.Executors;

public class Main {
    /*
    TODO Goal of this branch is to create a EventSourceFlushHandler that will append the logs in the root block file.
    We will extend this class with JSON something that will load the events from append only log and construct a json.
     */
    public static void main(String... args) {
        //TODO Progressive logging with fixed format which can be used to then generate different output. for client only. VFL Hub can still focus purely on its own logic. But the VFL Hub needs to support importing the client log
        //Flush at interval
        //TODO logger can take string and args of param to fill in those {}
        //TODO configuration using spring or file {@link https://claude.ai/chat/28eca1e0-9d4a-4465-836a-5d1feed5a3c4}
        var a = new ThreadSafeInMemoryFlushHandlerImpl();
        ThreadVFL.Runner.Instance.StartVFL("Main", new ThreadSafeAsyncVFLBuffer(1, 5000, a, Executors.newVirtualThreadPerTaskExecutor()), () -> {
            FluentThreadVFL.Call(() -> 1 * 2).asLog(integer -> "Result is {}");
        });
        System.out.println(a.generateNestedJsonStructure());
    }
}
