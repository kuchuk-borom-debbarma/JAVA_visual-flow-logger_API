package threadvfl;

import dev.kuku.vfl.core.buffer.AsyncBuffer;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.buffer.flushHandler.NestedJsonFlushHandler;
import dev.kuku.vfl.impl.threadlocal.FluentThreadVFLOps;
import dev.kuku.vfl.impl.threadlocal.ThreadVFLOps;
import dev.kuku.vfl.impl.threadlocal.logger.ThreadVFLRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

public class FluentThreadVFLOpsTest {
    int square(int a) {
        ThreadVFLOps.Log("Squaring " + a);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return FluentThreadVFLOps.Call(() -> a * a)
                .asLog("Square of {} is {}", a);
    }

    VFLBuffer createBuffer(String fileName) {
        NestedJsonFlushHandler f = new NestedJsonFlushHandler("test/output/" + this.getClass().getSimpleName() + "/" + fileName + ".json");
        return new AsyncBuffer(100, 3000, 100, f, Executors.newVirtualThreadPerTaskExecutor(), Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    void flatFlow() {
        ThreadVFLRunner.StartVFL("flat flow test", createBuffer("flatFlowTest"), () -> {
            FluentThreadVFLOps.Log("Starting {} linear flow", this.getClass().getSimpleName());
            FluentThreadVFLOps.Call(() -> square(2))
                    .asLog("Result is {}");
            FluentThreadVFLOps.Run(() -> square(3))
                    .andLog("GGEZ {}", "song");
        });
    }

    @Test
    void nest() {
        ThreadVFLRunner.StartVFL("nest test", createBuffer("nestTest"), () -> {
            FluentThreadVFLOps.Log("Starting {} nested flow", this.getClass().getSimpleName());

            FluentThreadVFLOps.Call(() -> square(3))
                    .asSubBlock("Sum block")
                    .withStartMessage("Squaring {}", 3)
                    .withEndMessage("Result of {} is {}", 3)
                    .execute();

            FluentThreadVFLOps.Run(() -> square(4))
                    .asSubBlock("Sum run block")
                    .execute();

        });
    }
}
