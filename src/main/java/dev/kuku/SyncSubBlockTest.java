package dev.kuku;

import dev.kuku.vfl.VflClient;

public class SyncSubBlockTest {
    public final VflClient vflClient;

    public SyncSubBlockTest(VflClient vflClient) {
        this.vflClient = vflClient;
    }

    public void test() {
        var mainBlock = vflClient.startSubBlock("SyncSubBlockTest");
        mainBlock = mainBlock.log("Synchronised Sub Block Test test");
        //TODO rest
    }
}
