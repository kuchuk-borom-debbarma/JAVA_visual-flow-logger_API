package dev.kuku.vfl;


public class VflClientBuilder {
    public static Configuration start() {
        return new Configuration();
    }

    public static class Configuration {
        private int bufferSize = 50;

        public Configuration bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public VflClient build() {
            var buffer = new VflBuffer(bufferSize);
            return new VflClient(buffer);
        }
    }
}
