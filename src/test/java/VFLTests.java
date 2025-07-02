import dev.kuku.vfl.VFL;
import dev.kuku.vfl.BlockLogger;
import dev.kuku.vfl.buffer.DefaultBufferImpl;
import dev.kuku.vfl.models.VflLogType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class VFLTests {

    @Test
    public void simple() {
        var s = new SimpleFlow();
        s.orderProgram();
    }


}
