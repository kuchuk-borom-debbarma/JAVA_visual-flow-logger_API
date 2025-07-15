package dev.kuku.vfl.core;


import dev.kuku.vfl.DummyRecord;

public class ScopedBlockLogImpl {
    private final ScopedValue<DummyRecord> s = ScopedValue.newInstance();

    public void start(Runnable a, String name, String title) {
        ScopedValue.where(s, new DummyRecord(name, title)).run(a);
    }

    public void text(String name) {
        System.out.println("CURRENT : " + s.get().toString());
        s.get().name = name;
        System.out.println("NEW : " + s.get().toString());

    }
}
