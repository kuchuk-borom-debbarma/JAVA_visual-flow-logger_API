package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.buffer.flushHandler.VFLHubFlushHandler;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.impl.annotation.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.Executors;

public class AnnotationTest {
    static VFLBuffer b;

    static VFLBuffer createBuffer(String fileName) {
        VFLFlushHandler f = new VFLHubFlushHandler(URI.create("http://localhost:8080"));
        return new AsyncBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void linear() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("linear")));
        new TestService().linear();
    }

    @Test
    void async() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("async")));
        new TestService().async();
    }

    @Test
    void eventListenerSyncTest() {
        VFLInitializer.initialize(new VFLAnnotationConfig(false, createBuffer("eventListenerSyncTest")));
        new TestService().eventPublisher();
    }

}

class TestService {
    @SubBlock(
            blockName = "block name is square {0}",
            startMessage = "squaring {0}",
            endMessage = "returned value is {r} for {0}"
    )
    private int square(int a) {
        return a * a;
    }

    @SubBlock
    private int squareAndMultiply(int a, int b) {
        int num = a * b;
        return square(num);
    }

    @SubBlock
    public void linear() {
        VFLStarter.StartRootBlock("Linear operation", () -> {
            Log.Info("SUP");
            int a = Log.InfoFn(() -> square(12), "Squaring {} = {}", 12);
            int b = squareAndMultiply(a, 2);
            Log.Info("COMPLETE");
        });
    }

    public void async() {
        VFLStarter.StartRootBlock("Async operation", () -> {
            Log.Info("Starting async test with thread pool");
            var e = Executors.newFixedThreadPool(1);
            var t = VFLFutures.runAsync(() -> {
                Log.Info("CRASH");
                square(1);
            });
            var t2 = VFLFutures.runAsync(() -> square(1), e);
            var y = VFLFutures.supplyAsync(() -> {
                Log.Info("Returning stuff");
                return square(2);
            }, e);

            t.join();
            t2.join();
            int num = y.join();
            Log.Info("COMPLETE with num {}", num);
        });
    }

    public void eventPublisher() {
        VFLStarter.StartRootBlock("Event publisher", () -> {
            Log.Info("Starting event listener test");
            System.err.println("Debug - About to publish event");
            var p = Log.Publish("Ordered item");
            System.err.println("Debug - Published event, result: " + p);
            Log.Info("Published stuff");
            listenerOne(p);
            Log.Info("Another log after listener 1");
            listenerTwo(p);
        });
    }


    void listenerOne(EventPublisherBlock p) {
        VFLStarter.StartEventListener(p, "Listener 1", null, () -> {
            Log.Info("Listener 1");
        });
    }

    void listenerTwo(EventPublisherBlock p) {
        VFLStarter.StartEventListener(p, "Listener 2", null, () -> {
            Log.Info("Listener 2");
        });
    }

}
