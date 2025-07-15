package dev.kuku;

import dev.kuku.vfl.core.builder.BlockLoggerBuilder;
import dev.kuku.vfl.core.logger.BlockLogger;

public class Main {
    private static final ScopedValue<BlockLogger> vflScope = ScopedValue.newInstance();

    public static void main(String... args) {
        var a = BlockLoggerBuilder.create().rootBlock("GGEZ").apiUrl("GGEZ").build();
        System.out.println(a);
    }
}


//TODO better safety using builder for everything