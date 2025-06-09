package dev.kuku;

import dev.kuku.vfl.VflClient;
import dev.kuku.vfl.VflClientBuilder;

public class Main {
    public static void main(String[] args) {
        var vflClient = VflClientBuilder.start().build();
        testSimple(vflClient);
    }

    //We need to update VFLBlockOperator per log
    public static void testSimple(VflClient vflClient) {
        var mainBlock = vflClient.startSubBlock("testSimple");
        mainBlock = mainBlock.log("Simple Logging test");
        mainBlock.log("Ending Simple logging test");
    }

    public static void testSyncSubBlock(VflClient vflClient) {
        var mainBlock = vflClient.startSubBlock("testSyncSubBlock");
        mainBlock = mainBlock.log("Testing Synchronised sub block");

    }
}