package dev.kuku.vfl.core.builder.apiBuilder;

import dev.kuku.vfl.core.serviceCall.NaiveVFLServerAPI;
import dev.kuku.vfl.core.serviceCall.VFLApi;

import java.util.Objects;

class VFLApiBuilderImpl implements FirstStep, SecondStep, ThirdStep {

    private String apiUrl;
    private VFLApi vflApi;

    @Override
    public SecondStep apiUrl(String url) {
        Objects.requireNonNull(url, "API URL can not be null");
        this.apiUrl = url;
        return this;
    }

    @Override
    public ThirdStep withNaiveServerApi() {
        vflApi = new NaiveVFLServerAPI(apiUrl);
        return this;
    }

    @Override
    public VFLApi build() {
        return vflApi;
    }
}
