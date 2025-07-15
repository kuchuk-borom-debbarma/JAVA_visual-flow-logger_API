package dev.kuku;

import dev.kuku.vfl.core.ScopedBlockLogImpl;

public class Main {
    private static final ScopedBlockLogImpl s = new ScopedBlockLogImpl();

    public static void main(String... args) {
        s.start(Main::root, "Kuku", "Debbarma");
    }

    static void root() {
        s.text("Kuchuk");
        s.start(Main::nested, "Lisa", "Jamatia");
    }

    static void nested() {
        s.text("IDK");
    }
}


//TODO better safety using builder for everything