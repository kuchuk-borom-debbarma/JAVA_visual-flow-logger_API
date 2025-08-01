package buffer;

import dev.kuku.vfl.core.buffer.AsyncVFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.VFLHubFlushHandler;
import dev.kuku.vfl.core.fluent_api.fn.FluentVFLFn;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;
import dev.kuku.vfl.variants.PassVFL;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.Executors;

public class VFLHubTest {
    private final VFLHubFlushHandler vflHubFlushHandler = new VFLHubFlushHandler(URI.create("http://localhost:8080"));
    private final AsyncVFLBuffer buffer = new AsyncVFLBuffer(10, 5000, 5000, vflHubFlushHandler, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());

    @Test
    void linearTest() {
        PassVFL.Runner.INSTANCE.StartVFL("linear test", buffer, vflFn -> {
            var f = new FluentVFLFn(vflFn);
            f.log("Starting linear test right now");
            f.log("Going to start another call right now");
            int result = f.startSubBlock(vflFn1 -> {
                        return square(vflFn1, 2);
                    }).withBlockName("Square block")
                    .withEndMessageMapper(integer -> "Squared result is {}")
                    .startPrimary();
            f.log("Finished linear test right now");
        });
    }

    int square(VFLFn l, int a) {
        var f = new FluentVFLFn(l);
        f.log("Starting square test right now");
        f.log("Squaring {}", a);
        return l.logFn(() -> a * a, integer -> "Square is {} of {}", a);
    }
}
