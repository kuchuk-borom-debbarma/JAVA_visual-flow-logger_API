package dev.kuku.vfl;


public final class DummyRecord {
    public String name;
    public String title;

    public DummyRecord(String name, String title) {
        this.name = name;
        this.title = title;
    }

    public String name() {
        return name;
    }

    public String title() {
        return title;
    }


    @Override
    public String toString() {
        return "DummyRecord[" +
                "name=" + name + ", " +
                "title=" + title + ']';
    }

}
