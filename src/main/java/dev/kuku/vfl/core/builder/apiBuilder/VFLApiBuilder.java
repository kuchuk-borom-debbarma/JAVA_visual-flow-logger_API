package dev.kuku.vfl.core.builder.apiBuilder;


/**
 * Builder class to build VFL API
 */
public class VFLApiBuilder {
    private VFLApiBuilder() {
    }

    public static FirstStep apiUrl(String url) {
        return new VFLApiBuilderImpl();
    }
}
